package api.entities.block

import entities.block.Block

object BlockMiner {

    fun mine(
        unminedBlock: Block,
        difficulty: Int,
    ): Block {
        val (currentNonce, currentHash) = findValidNonceAndHash(difficulty, unminedBlock)

        return Block(
            index = unminedBlock.index,
            timestamp = unminedBlock.timestamp,
            transactions = unminedBlock.transactions,
            previousHash = unminedBlock.previousHash,
            hash = currentHash,
            nonce = currentNonce,
        )
    }

    private fun findValidNonceAndHash(
        difficulty: Int,
        unminedBlock: Block,
    ): Pair<Long, String> {
        val target = "0".repeat(difficulty)

        var currentNonce = 0L
        var currentHash = unminedBlock.calculateHash(currentNonce)

        while (!currentHash.startsWith(target)) {
            currentNonce++
            currentHash = unminedBlock.calculateHash(currentNonce)
        }
        return Pair(currentNonce, currentHash)
    }
}
