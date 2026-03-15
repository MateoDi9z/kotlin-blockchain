package entities.block.rule

import entities.block.Block
import entities.results.ValidationResult

class ProofOfWorkRule : BlockRule {

    override fun validate(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): ValidationResult {
        val target = "0".repeat(difficulty)
        val isValid = block.hash.startsWith(target)

        return validationResult(isValid)
    }

    private fun validationResult(isValid: Boolean): ValidationResult =
        if (isValid) {
            ValidationResult(isValid = true, errorList = emptyList())
        } else {
            ValidationResult(
                isValid = false,
                errorList =
                    listOf(
                        "Validation Failed: Block does not meet the required difficulty.",
                    ),
            )
        }
}
