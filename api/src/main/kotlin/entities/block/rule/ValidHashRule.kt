package entities.block.rule

import entities.block.Block
import entities.results.ValidationResult

class ValidHashRule : BlockRule {

    override fun validate(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): ValidationResult {
        val computedHash = block.calculateHash()
        val isValid = block.hash == computedHash
        return result(isValid)
    }

    private fun result(isValid: Boolean): ValidationResult =
        if (isValid) {
            ValidationResult(isValid = true, errorList = emptyList())
        } else {
            ValidationResult(
                isValid = false,
                errorList =
                    listOf(
                        "Validation Failed: Block hash is invalid or data has " +
                            "been tampered with.",
                    ),
            )
        }
}
