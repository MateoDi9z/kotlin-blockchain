package api.entities

import api.dtos.Transaction

class Block(
    val index: Int,
    val timestamp: Long,
    val transactions: List<Transaction>,
    val previousHash: String,
    val hash: String,
    val nonce: Long = 0,
)
