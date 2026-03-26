from typing import Any

import argparse
import hashlib
import json
import time
import threading
from flask import Flask, jsonify, request
import requests as http_requests


# ---------------------------------------------------------------------------
# Blockchain Core
# ---------------------------------------------------------------------------

class Block:
    def __init__(self, index: int, timestamp: int, transactions, prev_hash, hash="", nonce=0):
        self.index = index
        self.timestamp = timestamp
        self.transactions: list[Transaction] = transactions
        self.prev_hash = prev_hash
        self.hash = hash
        self.nonce = nonce

    def to_dict(self):
        return {
            "index": self.index,
            "timestamp": self.timestamp,
            "transactions": [tx.to_dict() if hasattr(tx, "to_dict") else tx for tx in self.transactions],
            "previousHash": self.prev_hash,
            "hash": self.hash,
            "nonce": self.nonce
        }

    def __str__(self):
        return json.dumps(self.to_dict(), sort_keys=True)

    def __getitem__(self, key):
        return self.to_dict()[key]


class Transaction:
    def __init__(self, from_addr: str, to_addr: str, amount: float, sig: str):
        self.from_addr = from_addr
        self.to_addr = to_addr
        self.amount = amount
        self.sig = sig

    def to_dict(self):
        return {
            "from": self.from_addr,
            "to": self.to_addr,
            "amount": self.amount,
            "sig": self.sig
        }

    def __getitem__(self, key):
        return self.to_dict()[key]

    def __str__(self):
        return json.dumps(self.to_dict(), sort_keys=True)


DIFFICULTY = 3  # number of leading zeros required in block hash


def calculate_hash(index: int, timestamp: int, transactions: list, previous_hash: str, nonce: int) -> str:
    """SHA-256 hash of the block contents (everything except the hash itself)."""
    block_string = json.dumps(
        {
            "index": index,
            "timestamp": timestamp,
            "transactions": [tx.to_dict() if hasattr(tx, "to_dict") else tx for tx in transactions],
            "previousHash": previous_hash,
            "nonce": nonce,
        },
        sort_keys=True,
    )
    return hashlib.sha256(block_string.encode()).hexdigest()


def hash_valid(hash_value):
    return hash_value.startswith("0" * DIFFICULTY)


class Blockchain:
    def __init__(self):
        self.chain: list[Block] = []
        self.pending_transactions: list[Transaction] = []
        self.peers = set()
        self.lock = threading.Lock()
        self.port = None
        self._create_genesis_block()

    # -- Genesis block ------------------------------------------------------

    def _create_genesis_block(self):
        genesis = self._mine_raw_block(
            index=0,
            transactions=[],
            previous_hash="0" * 64,
            timestamp=0,
        )
        self.chain.append(genesis)

    # -- Mining -------------------------------------------------------------

    def _mine_raw_block(self, index: int, transactions: list, previous_hash: str, timestamp: int = None) -> Block:
        if timestamp is None:
            timestamp = int(time.time())
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
        with self.lock:
            txs = self.pending_transactions[:]
            self.pending_transactions = []
            last = self.chain[-1]

        block = self._mine_raw_block(
            index=last.index + 1,
            transactions=txs,
            previous_hash=last.hash
        )

        with self.lock:
            if block.prev_hash != self.chain[-1].hash:
                self.pending_transactions = txs + self.pending_transactions
                return None

            self.chain.append(block)
        return block

    def add_transaction(self, tx: Transaction):
        with self.lock:
            self.pending_transactions.append(tx)

    # -- Validation ---------------------------------------------------------

    @staticmethod
    def validate_block(block: Block, previous_block: Block):
        if block.index != previous_block.index + 1:
            return False
        if block.prev_hash != previous_block.hash:
            return False
        computed = calculate_hash(
            block.index,
            block.timestamp,
            block.transactions,
            block.prev_hash,
            block.nonce,
        )

        if computed != block.hash:
            return False

        if not hash_valid(block.hash):
            return False

        if block.timestamp > time.time() + 60:
            return False
        return True

    @staticmethod
    def validate_chain(chain: list[Block]):
        if not chain:
            return False
        for i in range(1, len(chain)):
            if not Blockchain.validate_block(chain[i], chain[i - 1]):
                return False
        return True

    def add_block(self, block: Any):
        """Attempt to add a single block received from a peer."""
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
            last = self.chain[-1]
            if not self.validate_block(block, last):
                return False
            self.chain.append(block)
            self.pending_transactions = [
                tx for tx in self.pending_transactions
                if tx not in block.transactions
            ]
            return True

    # -- Consensus ----------------------------------------------------------

    def resolve_conflicts(self):
        """Replace local chain with the longest valid chain among peers."""
        longest_chain = None
        max_length = len(self.chain)

        for peer in list(self.peers):
            try:
                resp = http_requests.get(f"{peer}/chain", timeout=5)

                if resp.status_code != 200:
                    continue

                data = resp.json()
                peer_chain_raw = data.get("chain", [])
                peer_chain = [Block(
                    b["index"],
                    b["timestamp"],
                    b["transactions"],
                    b["previousHash"],
                    b["hash"],
                    b["nonce"]
                ) if isinstance(b, dict) else b for b in peer_chain_raw]

                if len(peer_chain) > max_length and self.validate_chain(peer_chain):
                    max_length = len(peer_chain)
                    longest_chain = peer_chain
            except Exception:
                continue

        if not longest_chain:
            return False
        print(f"  Replacing local chain with longer chain from peer (length {max_length})")
        with self.lock:
            self.chain = longest_chain
        return True

    # -- P2P helpers --------------------------------------------------------

    def broadcast_block(self, block):
        if isinstance(block, Block):
            block = block.to_dict()

        for peer in list(self.peers):
            try:
                http_requests.post(
                    f"{peer}/block/new",
                    json=block,
                    timeout=5,
                )

            except Exception:
                self.peers.remove(peer)
                continue

    def register_peers(self, peer_urls):
        for peer_url in peer_urls:
            if peer_url in self.peers:
                continue
            self.peers.add(peer_url.rstrip("/"))


# ---------------------------------------------------------------------------
# Flask API
# ---------------------------------------------------------------------------

blockchain = Blockchain()
app = Flask(__name__)

# Configure logging to minimize output
import logging

app.logger.setLevel(logging.WARNING)
logging.getLogger('werkzeug').setLevel(logging.WARNING)


@app.route("/chain", methods=["GET"])
def get_chain():
    with blockchain.lock:
        chain = [block.to_dict() if isinstance(block, Block) else block for block in blockchain.chain]

    return jsonify({"chain": chain, "length": len(chain)})


@app.route("/peers", methods=["GET"])
def get_peers():
    return jsonify({"peers": list(blockchain.peers)})


# To register a peer, send a POST request with JSON body: {"peer": "http://localhost:5001"}
@app.route("/peers", methods=["POST"])
def register_peer():
    data = request.get_json(force=True)
    peer = data.get("peer")
    if not peer:
        return jsonify({"error": "peer field required"}), 400
    blockchain.register_peers([peer])
    return jsonify({"message": f"Peer {peer} registered", "peers": list(blockchain.peers)})


@app.route("/pending", methods=["GET"])
def get_pending():
    with blockchain.lock:
        pending = [tx.to_dict() if hasattr(tx, "to_dict") else tx for tx in blockchain.pending_transactions]
    return jsonify({"pending_transactions": pending, "count": len(pending)})


@app.route("/tx", methods=["POST"])
def new_transaction():
    data = request.get_json(force=True)

    if not all(k in data for k in ("from", "to", "amount", "sig")):
        return jsonify({"error": "Missing fields: from, to, amount, sig"}), 400

    tx = Transaction(
        from_addr=data["from"],
        to_addr=data["to"],
        amount=data["amount"],
        sig=data["sig"]
    )
    blockchain.add_transaction(tx)
    return jsonify({"message": "Transaction added to pool", "transaction": tx.to_dict()})


@app.route("/mine", methods=["POST"])
def mine():
    if len(blockchain.pending_transactions) == 0:
        return jsonify({"error": "No transactions to mine"}), 400

    block = blockchain.mine_block()

    if block is None:
        return jsonify({"error": "Mining failed – chain changed during mining"}), 409

    blockchain.broadcast_block(block)
    return jsonify(
        {"message": "Block mined successfully", "block": block.to_dict() if isinstance(block, Block) else block})


@app.route("/block/new", methods=["POST"])
def receive_block():
    block = request.get_json(force=True)
    required = ["index", "timestamp", "transactions", "previousHash", "hash", "nonce"]

    if not all(k in block for k in required):
        return jsonify({"error": "Missing fields: index, timestamp, transactions, previousHash, hash, nonce"}), 400

    with blockchain.lock:
        local_index = blockchain.chain[-1].index

    if block["index"] == local_index + 1:
        added = blockchain.add_block(block)

        if added:
            return jsonify({"message": "Block accepted"})

        return jsonify({"error": "Block rejected – invalid"}), 400

    elif block["index"] > local_index + 1:
        blockchain.resolve_conflicts()
        return jsonify({"message": "Chain was behind – resolved via consensus"})

    else:
        return jsonify({"message": "Block already known"}), 200


@app.route("/resolve", methods=["GET"])
def consensus():
    replaced = blockchain.resolve_conflicts()
    with blockchain.lock:
        chain = list(blockchain.chain)
    if replaced:
        return jsonify({"message": "Chain replaced", "chain": chain})
    return jsonify({"message": "Local chain is authoritative", "chain": chain})


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------

def resolve_periodically(interval=30):
    while True:
        blockchain.resolve_conflicts()
        time.sleep(interval)


cmd_functions = {
    "tx": lambda args: blockchain.add_transaction(Transaction(args[0], args[1], float(args[2]), args[3])),
    "pt": lambda args: print(
        f"Pending transactions: {[tx.to_dict() if hasattr(tx, 'to_dict') else tx for tx in blockchain.pending_transactions]}"),
    "mine": lambda args: http_requests.post(f"http://localhost:{blockchain.port}/mine", timeout=10),
    "r": lambda args: blockchain.resolve_conflicts(),
    "chain": lambda args: print(
        f"Chain: {[block.to_dict() if isinstance(block, Block) else block for block in blockchain.chain]}"),
    "help": lambda args: print_help(),
    "reg": lambda args: register_peer_handler(args),
    "peers": lambda args: print(f"Peers: {list(blockchain.peers)}"),
    "s": lambda args: print_status(),
    "port": lambda args: print(f"Port: {blockchain.port}")
}

def get_my_ip():
    import socket
    
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))  # No hace falta que exista conexión real
    ip_local = s.getsockname()[0]
    s.close()
    
    return ip_local

def register_peer_handler(args):
    if not args or not len(args):
        return
    
    my_ip = get_my_ip()
    print(f"Registering peer: {args[0]}")
    my_url = f"http://{my_ip}:{blockchain.port}"
    
    try:
        x = http_requests.post(
            f"{args[0]}/peers", 
            json={"peer": my_url},
            timeout=5) 
        
    except Exception as e:
        print(f"Error registering peer: {e}")
        return False
    
    y = blockchain.register_peers([args[0]])
    return x and y

def print_help():
    print(
        f"  Current chain length: {len(blockchain.chain)}, pending transactions: {len(blockchain.pending_transactions)}")
    print(f"  Latest block hash: {blockchain.chain[-1]['hash']}, nonce: {blockchain.chain[-1]['nonce']}")
    print(f"  Peers: {list(blockchain.peers)}")
    print(f"  To make a transaction type tx with args from, to, amount, sig (e.g. tx alice bob 10 signature)")
    print(f"  To show pending transactions type pt")
    print(f"  To mine a block type mine")
    print(f"  To resolve conflicts type r")
    print(f"  To show chain type chain")
    print(f"  To register a peer type reg with the peer URL (e.g. reg http://localhost:5001)")
    print(f"  To show this help message type help")
    print(f"  To exit type q")


def print_status():
    print(
        f"  Current chain length: {len(blockchain.chain)}, pending transactions: {len(blockchain.pending_transactions)}")
    print(f"  Latest block hash: {blockchain.chain[-1]['hash']}, nonce: {blockchain.chain[-1]['nonce']}")
    print(f"  Peers: {list(blockchain.peers)}")


def cli_loop():
    while True:
        time.sleep(1)

        cmd = input("Enter command: ").strip().lower()
        cmd_parts = cmd.split()

        if not cmd_parts:
            continue

        cmd_key = cmd_parts[0]
        cmd_args = cmd_parts[1:]

        cmd_functions.get(cmd_key, lambda args: print("Unknown command"))(cmd_args)
        if cmd_key == "q":
            print("Exiting...")
            break


def main():
    parser = argparse.ArgumentParser(description="Blockchain P2P Node")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host to listen on")
    parser.add_argument("--port", type=int, default=5001, help="Port to listen on")
    parser.add_argument(
        "--peers",
        type=str,
        default="",
        help="Comma-separated list of peer URLs (e.g. http://localhost:5001,http://localhost:5002)",
    )
    args = parser.parse_args()

    if args.peers:
        for p in args.peers.split(","):
            p = p.strip()

            if not p: continue

            blockchain.register_peers([p])

    print(f"  Starting node on port {args.port}")
    blockchain.port = args.port
    print(f"  Peers: {list(blockchain.peers)}")
    print(f"  Difficulty: {DIFFICULTY} (leading zeros)")
    print(f"  Genesis block hash: {blockchain.chain[0]['hash']}")

    import threading

    resolve_task = threading.Thread(target=resolve_periodically, kwargs={"interval": 30}, daemon=True)
    app_task = threading.Thread(target=app.run,
                                kwargs={"host": args.host, "port": args.port, "debug": False, "use_reloader": False},
                                daemon=True)

    resolve_task.start()
    app_task.start()

    cli_loop()

    # wait for the cli task terminate (when user types 'q')
    print("Shutting down node...")
    resolve_task.join(timeout=.1)
    app_task.join(timeout=.1)


if __name__ == "__main__":
    main()