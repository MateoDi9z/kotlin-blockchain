package org.example.crypto.merkle

import org.example.core.transaction.Transaction
import api.entities.crypto.hash.Hash

class MerkleTree {
    private var rootHash: String = ""

    fun init(txs: List<Transaction>) {
        if (txs.isEmpty()) {
            this.rootHash = ""
            return
        }

        val txQueue = ArrayDeque<String>()

        for (tx in txs) {
            txQueue.addLast(tx.hash())
        }

        while (txQueue.size > 1) {
            val levelSize = txQueue.size

            // si el nivel es impar duplicamos el último
            if (levelSize % 2 == 1) {
                txQueue.addLast(txQueue.last())
            }

            val nextLevel = ArrayDeque<String>()

            while (txQueue.isNotEmpty()) {
                val tx1 = txQueue.removeFirst()
                val tx2 = txQueue.removeFirst()

                val combinedHash = Hash.sha256(tx1 + tx2)
                nextLevel.addLast(combinedHash)
            }

            txQueue.clear()
            txQueue.addAll(nextLevel)
        }

        this.rootHash = txQueue.last()
    }

    fun getRootHash(): String = rootHash
}
