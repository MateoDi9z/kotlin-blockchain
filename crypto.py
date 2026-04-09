import base64
from eth_account import Account
from eth_account.messages import encode_defunct
from eth_keys import keys


def create_wallet():
    acct = Account.create()

    return {
        "private_key": acct.key.hex(),
        "address": acct.address,
        "public_key": acct._key_obj.public_key.to_hex()
    }


def get_address_from_public_key(public_key_hex: str) -> str:
    if public_key_hex.startswith("0x"):
        public_key_hex = public_key_hex[2:]

    pk_bytes = bytes.fromhex(public_key_hex)
    public_key_obj = keys.PublicKey(pk_bytes)

    return public_key_obj.to_address().lower()


def validate_from_matches_public_key(from_address: str, public_key_hex: str) -> bool:
    try:
        derived_address = get_address_from_public_key(public_key_hex)

        return from_address.lower() == derived_address.lower()
    except Exception:
        return False

def get_canonical_payload(from_addr: str, to_addr: str, amount: int, timestamp: int) -> str:
    return f"TRANSFER|{from_addr}|{to_addr}|{amount}|{timestamp}"


def sign_payload(private_key_hex: str, payload: str) -> str:
    message = encode_defunct(text=payload)

    signed_message = Account.sign_message(message, private_key=private_key_hex)

    signature_b64 = base64.b64encode(signed_message.signature).decode('utf-8')
    return signature_b64


def verify_signature(payload: str, signature_b64: str, expected_address: str) -> bool:
    try:
        signature_bytes = base64.b64decode(signature_b64)

        message = encode_defunct(text=payload)

        recovered_address = Account.recover_message(message, signature=signature_bytes)

        return recovered_address.lower() == expected_address.lower()
    except Exception as e:
        return False