import threading
import time
import uuid
import requests as http_requests

from typing import Any
from crypto import get_canonical_payload, verify_signature, validate_from_matches_public_key

from models import Block, Transaction
from utils import (
    calculate_hash,
    hash_valid,
    TRANSACTION_TYPE,
    AUTO_MINE_THRESHOLD,
    BLOCK_REWARD,
    is_placeholder_zeros,
)


class Blockchain:
    def __init__(self):
        self.chain: list[Block] = []
        self.pending_transactions: list[Transaction] = []
        self.peers = set()

        self.seen_transactions = set()
        self.seen_blocks = set()

        self.lock = threading.Lock()
        self.port = None
        self.miner_address: str | None = None
        self.miner_public_key: str | None = None
        self.miner_private_key: str | None = None
        self._create_genesis_block()

    def set_node_identity(
        self,
        address: str,
        public_key_hex: str,
        private_key_hex: str | None = None,
    ) -> None:
        """Dirección y claves del nodo (minería, /status, firma de tx desde CLI)."""
        a = (address or "").strip()
        self.miner_address = a.lower()
        pk = (public_key_hex or "").strip()
        self.miner_public_key = pk[2:] if pk.startswith("0x") else pk
        if private_key_hex:
            sk = private_key_hex.strip()
            self.miner_private_key = sk if sk.startswith("0x") else f"0x{sk}"
        else:
            self.miner_private_key = None

    # Genesis block

    def _create_genesis_block(self):
        genesis = self._mine_raw_block(
            index=0,
            transactions=[],
            previous_hash="0",
            timestamp=1,
        )
        self.chain.append(genesis)
        self.seen_blocks.add(genesis.hash)

    # Mining

    def _mine_raw_block(self, index: int, transactions: list, previous_hash: str, timestamp: int = None) -> Block:
        if timestamp is None:
            timestamp = int(time.time() * 1000)
        nonce = 0
        h = calculate_hash(
            index,
            timestamp,
            transactions,
            previous_hash,
            nonce
        )

        while not hash_valid(h):
            nonce += 1
            h = calculate_hash(
                index,
                timestamp,
                transactions,
                previous_hash,
                nonce
            )

        return Block(
            index=index,
            timestamp=timestamp,
            transactions=transactions,
            prev_hash=previous_hash,
            hash=h,
            nonce=nonce
        )

    def mine_block(self):
        if not self.miner_address:
            raise RuntimeError("Node identity not set; call set_node_identity before mining")

        with self.lock:
            block_timestamp = int(time.time() * 1000)

            coinbase_tx = Transaction(
                from_addr="SYSTEM",
                to_addr=self.miner_address,
                amount=BLOCK_REWARD,
                public_key="0" * 64,
                signature="0" * 64,
                tx_type=TRANSACTION_TYPE.COINBASE,
                timestamp=block_timestamp
            )

            txs = [coinbase_tx] + self.pending_transactions[:]

            self.pending_transactions = []
            last = self.chain[-1]

        block = self._mine_raw_block(
            index=last.index + 1,
            transactions=txs,
            previous_hash=last.hash,
            timestamp=block_timestamp
        )

        with self.lock:
            if block.prev_hash != self.chain[-1].hash:
                # Chain changed while mining, restore mempool
                self.pending_transactions = txs[1:] + self.pending_transactions
                return None

            self.chain.append(block)
            self.seen_blocks.add(block.hash)  # Cache the newly mined block

        return block

    def _pending_transfer_count(self) -> int:
        return sum(
            1
            for tx in self.pending_transactions
            if (tx.type if hasattr(tx, "type") else tx.get("type")) == TRANSACTION_TYPE.TRANSFER
        )

    def _auto_mine_and_broadcast(self):
        with self.lock:
            if self._pending_transfer_count() < AUTO_MINE_THRESHOLD:
                return

        block = self.mine_block()

        if block:
            self.broadcast_block(block)

    def add_transaction(self, tx: Transaction):
        tx_id = tx.id if hasattr(tx, "id") else tx.get("id")

        with self.lock:
            if tx_id in self.seen_transactions:
                return False

            if not self.validate_transaction(tx):
                return False

            self.pending_transactions.append(tx)
            self.seen_transactions.add(tx_id)

            pending_transfer_count = self._pending_transfer_count()

        threading.Thread(target=self.broadcast_transaction, args=(tx,), daemon=True).start()

        if pending_transfer_count >= AUTO_MINE_THRESHOLD:
            threading.Thread(target=self._auto_mine_and_broadcast, daemon=True).start()

        return True

    @staticmethod
    def _validate_ownership(tx) -> bool:
        """Validate that: from == address(publicKey)"""
        return validate_from_matches_public_key(tx.from_addr, tx.public_key)

    @staticmethod
    def _validate_signature(tx) -> bool:
        """Verify the cryptographic signature with the canonical payload"""
        payload = get_canonical_payload(
            tx.from_addr,
            tx.to_addr,
            tx.amount,
            tx.timestamp
        )
        return verify_signature(payload, tx.signature, tx.from_addr)


    def get_balance(self, address: str) -> int:
        address = (address or "").lower()
        balance = self.get_chain_balance(address)

        for tx in self.pending_transactions:
            tx_from = tx.get("from") if isinstance(tx, dict) else tx.from_addr
            tx_amount = tx.get("amount") if isinstance(tx, dict) else tx.amount

            if (tx_from or "").lower() == address:
                balance -= tx_amount

        return balance

    def get_chain_balance(self, address: str) -> int:
        address = (address or "").lower()
        balance = 0
        for block in self.chain:
            for tx in block.transactions:
                tx_from = tx.get("from") if isinstance(tx, dict) else getattr(tx, "from_addr", None)
                tx_to = tx.get("to") if isinstance(tx, dict) else getattr(tx, "to_addr", None)
                tx_amount = tx.get("amount") if isinstance(tx, dict) else getattr(tx, "amount", 0)

                if (tx_to or "").lower() == address:
                    balance += tx_amount
                if (tx_from or "").lower() == address:
                    balance -= tx_amount
        return balance

    @staticmethod
    def _validate_basic_rules(tx) -> bool:
        """Basic logical rules: amount > 0 and from != to"""
        if tx.amount <= 0:
            return False

        if tx.from_addr == tx.to_addr:
            return False

        return True

    def _validate_balance(self, tx) -> bool:
        """Verify that the sender has sufficient funds"""
        if self.get_balance(tx.from_addr) < tx.amount:
            return False
        return True

    @staticmethod
    def _is_valid_uuid4(tx_id: str) -> bool:
        try:
            return uuid.UUID(tx_id).version == 4
        except (ValueError, TypeError, AttributeError):
            return False

    def validate_transaction(self, tx) -> bool:
        if tx.type == TRANSACTION_TYPE.COINBASE:
            return True

        if tx.type != TRANSACTION_TYPE.TRANSFER:
            return False

        if not tx.id or not self._is_valid_uuid4(str(tx.id)):
            return False

        if tx.timestamp is None or int(tx.timestamp) <= 0:
            return False

        if not tx.public_key or not tx.signature:
            return False

        if not self._validate_basic_rules(tx):
            return False

        if not self._validate_ownership(tx):
            return False

        if not self._validate_signature(tx):
            return False

        if not self._validate_balance(tx):
            return False

        return True

    # -- Block and chain validation ----------------------------------------

    def validate_block(self, block: Block, previous_block: Block = None, external_balances: dict = None, is_full_chain_validation: bool = False):
        if block.index < 0: return False
        if block.timestamp <= 0: return False
        if block.transactions is None: return False
        if block.prev_hash is None: return False
        if block.hash is None: return False
        if block.nonce < 0: return False

        computed = calculate_hash(
            block.index, block.timestamp, block.transactions, block.prev_hash, block.nonce
        )
        if computed != block.hash: return False
        if not hash_valid(block.hash): return False

        if block.index == 0:
            if block.prev_hash != "0": return False
            if len(block.transactions) != 0: return False
            return True

        if previous_block is None: return False
        if block.index != previous_block.index + 1: return False
        if block.prev_hash != previous_block.hash: return False
        if block.timestamp <= previous_block.timestamp: return False

        if len(block.transactions) == 0: return False

        _tx_attr = {"from": "from_addr", "to": "to_addr", "publicKey": "public_key"}

        def get_tx_field(tx, field):
            if isinstance(tx, dict):
                return tx.get(field)
            attr = _tx_attr.get(field, field)
            return getattr(tx, attr, None)

        first_tx = block.transactions[0]

        if get_tx_field(first_tx, 'type') != TRANSACTION_TYPE.COINBASE: return False
        if get_tx_field(first_tx, 'from') != "SYSTEM": return False
        if int(get_tx_field(first_tx, 'amount')) != BLOCK_REWARD: return False
        if get_tx_field(first_tx, 'timestamp') != block.timestamp: return False

        pk_cb = get_tx_field(first_tx, 'publicKey')
        sig_cb = get_tx_field(first_tx, 'signature')
        if pk_cb is None or sig_cb is None:
            return False
        if not is_placeholder_zeros(str(pk_cb)) or not is_placeholder_zeros(str(sig_cb)):
            return False

        coinbase_count = sum(1 for tx in block.transactions if get_tx_field(tx, 'type') == TRANSACTION_TYPE.COINBASE)
        if coinbase_count != 1: return False

        # --- NUEVA SIMULACIÓN DE ESTADO Y VALIDACIÓN ESTRICTA ---
        simulated_balances = external_balances.copy() if external_balances is not None else {}

        def get_simulated_balance(addr):
            a = (addr or "").lower()
            if a not in simulated_balances:
                if is_full_chain_validation:
                    simulated_balances[a] = 0
                else:
                    simulated_balances[a] = self.get_chain_balance(a)
            return simulated_balances[a]

        # 1. Sumamos la recompensa de minado de la COINBASE al nodo minero
        miner_addr = (get_tx_field(first_tx, "to") or "").lower()
        miner_amount = int(get_tx_field(first_tx, "amount"))
        simulated_balances[miner_addr] = get_simulated_balance(miner_addr) + miner_amount

        # 2. Validamos el resto de las transacciones (TRANSFER)
        for tx in block.transactions[1:]:
            if get_tx_field(tx, 'type') != TRANSACTION_TYPE.TRANSFER: return False

            if isinstance(tx, dict):
                tx_obj = Transaction(
                    from_addr=tx.get("from"), to_addr=tx.get("to"), amount=tx.get("amount"),
                    public_key=tx.get("publicKey"), signature=tx.get("signature"),
                    tx_type=tx.get("type"), tx_id=tx.get("id"), timestamp=tx.get("timestamp")
                )
            else:
                tx_obj = tx

            if not tx_obj.id or not self._is_valid_uuid4(str(tx_obj.id)):
                return False
            if tx_obj.timestamp is None or int(tx_obj.timestamp) <= 0:
                return False
            if not tx_obj.public_key or not tx_obj.signature:
                return False

            # Validación de propiedades intrínsecas
            if not self._validate_basic_rules(tx_obj): return False
            if not self._validate_ownership(tx_obj): return False
            if not self._validate_signature(tx_obj): return False

            f = (tx_obj.from_addr or "").lower()
            t = (tx_obj.to_addr or "").lower()
            sender_balance = get_simulated_balance(f)
            if sender_balance < tx_obj.amount:
                return False

            simulated_balances[f] -= tx_obj.amount
            simulated_balances[t] = get_simulated_balance(t) + tx_obj.amount

        # Guardar estado por si estamos validando múltiples bloques (validate_chain)
        if external_balances is not None:
            external_balances.update(simulated_balances)

        return True

    def validate_chain(self, chain: list[Block]):
        if not chain:
            return False

        if not self.validate_block(chain[0], None):
            return False

        state_balances = {}

        for i in range(1, len(chain)):
            # Validamos cada bloque suministrando los balances arrastrados
            if not self.validate_block(chain[i], chain[i - 1], external_balances=state_balances, is_full_chain_validation=True):
                return False

        return True

    def add_block(self, block: Any):
        if isinstance(block, dict):
            block = Block(
                block["index"],
                block["timestamp"],
                block["transactions"],
                block["previousHash"],
                block["hash"],
                block["nonce"]
            )

        with self.lock:
            block_hash = block.hash

            # Check cache to avoid gossip loops
            if block_hash in self.seen_blocks:
                return False

            last = self.chain[-1]
            if not self.validate_block(block, last):
                return False

            self.chain.append(block)
            self.seen_blocks.add(block_hash)

            # Remove mined transactions from mempool
            mined_tx_ids = [tx["id"] if isinstance(tx, dict) else tx.id for tx in block.transactions]
            self.pending_transactions = [
                tx for tx in self.pending_transactions
                if (tx.get("id") if isinstance(tx, dict) else getattr(tx, "id")) not in mined_tx_ids
            ]

        # Successfully added to chain, broadcast to peers
        threading.Thread(target=self.broadcast_block, args=(block,), daemon=True).start()
        return True

    # Consenso

    @staticmethod
    def _blocks_from_chain_json(peer_chain_raw: list) -> list:
        return [
            Block(
                b["index"],
                b["timestamp"],
                b["transactions"],
                b["previousHash"],
                b["hash"],
                b["nonce"],
            )
            if isinstance(b, dict)
            else b
            for b in peer_chain_raw
        ]

    def _replace_chain_if_valid_and_longer(self, candidate: list) -> bool:
        if not candidate or len(candidate) <= len(self.chain):
            return False
        if not self.validate_chain(candidate):
            return False
        print(f"  Replacing local chain with longer valid chain (length {len(candidate)})")
        with self.lock:
            self.chain = candidate
            for b in self.chain:
                self.seen_blocks.add(b.hash)
        return True

    def apply_chain_from_http_response(self, data: dict) -> bool:
        peer_chain_raw = data.get("chain", [])
        peer_chain = self._blocks_from_chain_json(peer_chain_raw)
        return self._replace_chain_if_valid_and_longer(peer_chain)

    def resolve_conflicts(self):
        longest_chain = None
        max_length = len(self.chain)

        for peer in list(self.peers):
            try:
                resp = http_requests.get(f"{peer}/chain", timeout=5)
                if resp.status_code != 200:
                    continue
                peer_chain = self._blocks_from_chain_json(resp.json().get("chain", []))
                if len(peer_chain) > max_length and self.validate_chain(peer_chain):
                    max_length = len(peer_chain)
                    longest_chain = peer_chain
            except Exception:
                continue

        if not longest_chain:
            return False

        with self.lock:
            self.chain = longest_chain
            for b in self.chain:
                self.seen_blocks.add(b.hash)
        return True

    # P2P helpers (Gossip Protocol)

    def broadcast_block(self, block):
        """Broadcasts a block to all registered peers."""
        block_dict = block.to_dict() if isinstance(block, Block) else block

        for peer in list(self.peers):
            try:
                http_requests.post(
                    f"{peer}/blocks",
                    json=block_dict,
                    timeout=5,
                )
            except Exception:
                continue

    def broadcast_transaction(self, tx):
        tx_dict = tx.to_dict() if hasattr(tx, "to_dict") else tx

        for peer in list(self.peers):
            try:
                http_requests.post(
                    f"{peer}/transactions",
                    json=tx_dict,
                    timeout=5,
                )
            except Exception:
                continue

    def register_peers(self, peer_urls):
        for peer_url in peer_urls:
            if peer_url in self.peers:
                continue
            self.peers.add(peer_url.rstrip("/"))


# Global blockchain instance
blockchain = Blockchain()