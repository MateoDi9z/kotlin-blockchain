from flask import Flask, jsonify, request
import logging

from blockchain import blockchain
from models import Block, Transaction
from utils import TRANSACTION_TYPE, get_my_ip


class ApiResponse:
    def ok(self, data=None):
        """Genera el formato de éxito. Si hay data, la fusiona en la raíz."""
        response = {"status": "ok"}
        if data is not None:
            response.update(data)
        return jsonify(response)

    def error(self, code: str, message: str):
        """Genera estrictamente el formato de error de la sección 9.1 del TP"""
        return jsonify({
            "status": "error",
            "error": {
                "code": code,
                "message": message
            }
        })


app = Flask(__name__)

# Configure logging to minimize output
app.logger.setLevel(logging.WARNING)
logging.getLogger('werkzeug').setLevel(logging.WARNING)


@app.route("/chain", methods=["GET"])
def get_chain():
    with blockchain.lock:
        chain = [block.to_dict() if isinstance(block, Block) else block for block in blockchain.chain]
    return ApiResponse().ok({"chain": chain, "length": len(chain)})


@app.route("/peers", methods=["GET"])
def get_peers():
    return ApiResponse().ok({
        "peers": list(blockchain.peers),
        "count": len(blockchain.peers)
    })


@app.route("/peers", methods=["POST"])
def register_peer():
    data = request.get_json(force=True)
    peer = data.get("peer") or data.get("url")

    if not peer:
        return ApiResponse().error("MISSING_PEER", "peer field required"), 400

    blockchain.register_peers([peer])
    return ApiResponse().ok({
        "registered": peer,
        "peers": list(blockchain.peers)
    })


@app.route("/pending", methods=["GET"])
def get_pending():
    with blockchain.lock:
        pending = [tx.to_dict() if hasattr(tx, "to_dict") else tx for tx in blockchain.pending_transactions]
    return ApiResponse().ok({"pending_transactions": pending, "count": len(pending)})


@app.route("/transactions", methods=["POST"])
def new_transaction():
    data = request.get_json(force=True)

    if not all(k in data for k in ("from", "to", "amount", "signature", "publicKey", "timestamp")):
        return ApiResponse().error("MISSING_FIELDS", "Missing fields in transaction"), 400

    # Evitamos que entren COINBASE por la API (Regla TP1)
    if data.get("type") == TRANSACTION_TYPE.COINBASE:
        return ApiResponse().error("INVALID_TYPE", "COINBASE transactions are not accepted via API"), 400

    tx = Transaction(
        from_addr=data["from"],
        to_addr=data["to"],
        amount=int(data["amount"]),
        public_key=data["publicKey"],
        signature=data["signature"],
        tx_type=data.get("type", TRANSACTION_TYPE.TRANSFER),
        tx_id=data.get("id"),
        timestamp=data["timestamp"]
    )

    if blockchain.add_transaction(tx):
        return ApiResponse().ok({"accepted": True, "txId": tx.id}), 202
    else:
        return ApiResponse().error("REJECTED_TRANSACTION", "Transaction invalid or already processed"), 400


@app.route("/mine", methods=["POST"])
def mine():
    # Eliminada la restricción de len(pending_transactions) == 0
    block = blockchain.mine_block()

    if block is None:
        return ApiResponse().error("MINING_FAILED", "Mining failed – chain changed during mining"), 409

    blockchain.broadcast_block(block)

    data = {
        'mined': True,
        'trigger': 'manual',
        'block': block.to_dict() if isinstance(block, Block) else block
    }

    return ApiResponse().ok(data), 200


@app.route("/blocks", methods=["POST"])  # Cambiado de /block/new a /blocks según TP1
def receive_block():
    block = request.get_json(force=True)
    required = ["index", "timestamp", "transactions", "previousHash", "hash", "nonce"]

    if not all(k in block for k in required):
        return ApiResponse().error("MISSING_FIELDS", "Missing required block fields"), 400

    with blockchain.lock:
        local_index = blockchain.chain[-1].index

    if block["index"] == local_index + 1:
        added = blockchain.add_block(block)

        if added:
            return ApiResponse().ok({
                "accepted": True,
                "action": "appended",
                "chainLength": len(blockchain.chain)
            }), 200
        else:
            return ApiResponse().error("INVALID_BLOCK", "Block validation failed"), 400

    elif block["index"] > local_index + 1:
        blockchain.resolve_conflicts()
        return ApiResponse().ok({"accepted": False, "action": "resolved_via_consensus"}), 200

    else:
        return ApiResponse().ok({"accepted": False, "action": "ignored"}), 200


@app.route("/resolve", methods=["GET"])
def consensus():
    replaced = blockchain.resolve_conflicts()
    with blockchain.lock:
        chain = list(blockchain.chain)

    msg = "Chain replaced" if replaced else "Local chain is authoritative"
    return ApiResponse().ok({"message": msg, "chain": chain})


@app.route("/health", methods=["GET"])
def health():
    return ApiResponse().ok()


@app.route("/status", methods=["GET"])
def node_status():
    with blockchain.lock:
        chain_length = len(blockchain.chain)
        latest_hash = blockchain.chain[-1].hash if chain_length > 0 else ""

    return ApiResponse().ok({
        "node": {
            "url": f"http://{get_my_ip()}:{blockchain.port}",
            "address": "0x0000000000000000000000000000000000000000",
            "publickey": "0000000000000000000000000000000000000000000000000000000000000000"
        },
        "chain": {
            "length": chain_length,
            "latestHash": latest_hash
        },
        "peers": {
            "count": len(blockchain.peers)
        }
    }), 200