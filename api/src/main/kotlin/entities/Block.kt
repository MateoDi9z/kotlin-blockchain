package api.entities

import api.dtos.Transaction
import api.entities.crypto.hash.Hash

class Block(
    val index: Int,
    val timestamp: Long,
    val transactions: List<Transaction>,
    val previousHash: String,
    var hash: String = "",
    var nonce: Long = 0,
) {
    init {
        if (hash.isEmpty()) {
            hash = calculateHash()
        }
    }

    fun calculateHash(): String {
        val dataToHash = "$index$timestamp$transactions$previousHash$nonce"

        return Hash.sha256(dataToHash)
    }

    fun mineBlock(difficulty: Int) {
        val target = "0".repeat(difficulty)

        while (!hash.startsWith(target)) {
            nonce++
            hash = calculateHash()
        }
    }
}
