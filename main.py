import argparse
import threading

from api import app
from blockchain import blockchain
from cli import cli_loop, resolve_periodically
from utils import DIFFICULTY


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