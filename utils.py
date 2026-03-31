import hashlib
import json
import time


DIFFICULTY = 4  # number of leading zeros required in block hash


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