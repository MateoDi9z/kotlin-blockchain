import re
import time
import uuid
import socket
import requests as http_requests

from blockchain import blockchain
from crypto import get_canonical_payload, sign_payload
from models import Transaction, Block
from utils import TRANSACTION_TYPE


def resolve_periodically(interval=30):
    while True:
        blockchain.resolve_conflicts()
        time.sleep(interval)


def normalize_eth_address(addr: str) -> str | None:
    """Acepta 0x + 40 hex o solo 40 hex (el input del CLI ya viene en minúsculas)."""
    a = addr.strip().lower()
    if re.fullmatch(r"0x[0-9a-f]{40}", a):
        return a
    if re.fullmatch(r"[0-9a-f]{40}", a):
        return "0x" + a
    return None


def tx_handler(args):
    """TRANSFER firmada con la wallet del nodo: tx <to> <amount>"""
    if len(args) < 2:
        print("  Uso: tx <direccion_destino> <monto>")
        print("  Ejemplo: tx 0x14f63d9393d3b9d3182d245f3f5ddfba7b4452e1 7")
        return
    if not blockchain.miner_private_key or not blockchain.miner_address:
        print("  Error: no hay clave de firma del nodo (reinicia con NODE_PRIVATE_KEY o wallet generada).")
        return

    to_addr = normalize_eth_address(args[0])
    if not to_addr:
        print("  Error: dirección destino inválida (esperado 0x + 40 caracteres hex).")
        return

    try:
        amount = int(args[1])
    except ValueError:
        print("  Error: el monto debe ser un entero.")
        return

    if amount <= 0:
        print("  Error: el monto debe ser > 0.")
        return

    if to_addr == blockchain.miner_address:
        print("  Error: origen y destino no pueden coincidir.")
        return

    ts = int(time.time() * 1000)
    tx_id = str(uuid.uuid4())
    from_addr = blockchain.miner_address
    payload = get_canonical_payload(from_addr, to_addr, amount, ts)
    sig = sign_payload(blockchain.miner_private_key, payload)
    pub = blockchain.miner_public_key or ""
    pub_hex = pub if pub.startswith("0x") else f"0x{pub}"

    tx = Transaction(
        from_addr=from_addr,
        to_addr=to_addr,
        amount=amount,
        public_key=pub_hex,
        signature=sig,
        tx_type=TRANSACTION_TYPE.TRANSFER,
        tx_id=tx_id,
        timestamp=ts,
    )

    if blockchain.add_transaction(tx):
        print(f"  TX aceptada  id={tx_id}")
        print(f"  {from_addr} -> {to_addr}  amount={amount}")
    else:
        print("  TX rechazada (balance insuficiente, duplicada o validación fallida).")


def balance_handler(args):
    """Muestra el saldo de la wallet del nodo. Uso opcional: balance <0x...> para otra dirección."""
    if args:
        addr = normalize_eth_address(args[0])
        if not addr:
            print("  Error: dirección inválida. Uso: balance [0x...]")
            return
    else:
        addr = blockchain.miner_address
        if not addr:
            print("  Error: identidad del nodo no cargada.")
            return

    on_chain = blockchain.get_chain_balance(addr)
    spendable = blockchain.get_balance(addr)
    print(f"  Dirección: {addr}")
    print(f"  Confirmado en cadena: {on_chain}")
    print(f"  Disponible para gastar (tras pendientes): {spendable}")


cmd_functions = {
    "tx": tx_handler,
    "balance": balance_handler,
    "b": balance_handler,
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
            json={"url": my_url},
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
    print(f"  tx <direccion_0x> <monto>  — envía desde la wallet de este nodo (firmado automático)")
    print(f"  balance  o  b  — saldo de tu nodo;  balance <0x...>  — saldo de otra dirección")
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