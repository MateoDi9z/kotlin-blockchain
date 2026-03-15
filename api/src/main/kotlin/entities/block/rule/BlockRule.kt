package entities.block.rule

import entities.block.Block
import entities.results.ValidationResult

interface BlockRule {
    fun validate(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): ValidationResult
}
