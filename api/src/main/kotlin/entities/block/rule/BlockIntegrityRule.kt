package entities.block.rule

import entities.block.Block

class BlockIntegrityRule : BlockRule {

    override fun isValid(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): Boolean {
        val computedHash = block.calculateHash()
        val target = "0".repeat(difficulty)

        val hasValidHash = (block.hash == computedHash)
        val meetsDifficulty = block.hash.startsWith(target)

        return hasValidHash && meetsDifficulty
    }

    override fun getErrorMessage() = "Validation Failed: Data integrity or Difficulty not met."
}
