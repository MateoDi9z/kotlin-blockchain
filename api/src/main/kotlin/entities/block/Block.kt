package entities.block

import api.dtos.Transaction
import api.entities.hash.Hash

data class Block(
    val index: Int,
    val timestamp: Long,
    val transactions: List<Transaction>,
    val previousHash: String,
    val hash: String = "",
    val nonce: Long = 0,
) {

    fun calculateHash(nonceToUse: Long = this.nonce): String {
        val dataToHash = "$index$timestamp$transactions$previousHash$nonceToUse"
        return Hash.sha256(dataToHash)
    }
}
