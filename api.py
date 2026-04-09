from flask import Flask, jsonify, request
import logging

from blockchain import blockchain
from models import Block, Transaction

class ApiResponse:
    def __init__(self, status: str, message: str, data=None):
        self.status = status
        self.message = message
        self.data = data
    
    def __init__(self):
        self.status = ""
        self.message = ""
        self.data = None

    def to_dict(self):
        response = {"status": self.status, "message": self.message}
        if self.data is not None:
            response = {**response, **self.data}
        return jsonify(response)
    
    def ok(self, message: str, data=None):
        self.status = "ok"
        self.message = message
        self.data = data
        return self.to_dict()
    
    def error(self, message: str, data=None):
        self.status = "error"
        self.message = message
        self.data = data
        return self.to_dict()

app = Flask(__name__)

# Configure logging to minimize output
app.logger.setLevel(logging.WARNING)
logging.getLogger('werkzeug').setLevel(logging.WARNING)


@app.route("/chain", methods=["GET"])
def get_chain():
    with blockchain.lock:
        chain = [block.to_dict() if isinstance(block, Block) else block for block in blockchain.chain]

    return jsonify({"chain": chain, "length": len(chain)})


@app.route("/peers", methods=["GET"])
def get_peers():
    return jsonify({
        "status": "ok",
        "peers": list(blockchain.peers),
        "count": len(blockchain.peers)
    })


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
        return ApiResponse().error("No transactions to mine"), 400

    block = blockchain.mine_block()

    if block is None:
        return ApiResponse().error("Mining failed – chain changed during mining"), 409

    blockchain.broadcast_block(block)
    
    data = {
        'mined': True,
        'trigger': 'manual',
        'block': block.to_dict() if isinstance(block, Block) else block
    }
    
    return ApiResponse().ok("Block mined successfully", data), 200


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