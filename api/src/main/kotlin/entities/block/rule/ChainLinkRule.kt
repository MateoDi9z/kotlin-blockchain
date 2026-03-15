package entities.block.rule

import entities.block.Block
import entities.results.ValidationResult

class ChainLinkRule : BlockRule {
    override fun validate(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): ValidationResult {
        val isValid = block.previousHash == previousBlock.hash

        return if (isValid) {
            ValidationResult(isValid = true, errorList = emptyList())
        } else {
            ValidationResult(
                isValid = false,
                errorList = listOf("Validation Failed: Previous hash mismatch."),
            )
        }
    }
}
