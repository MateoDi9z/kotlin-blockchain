package entities.block.rule

import entities.block.Block
import entities.results.ValidationResult

class CompositeBlockRule(
    private val rules: List<BlockRule>,
) : BlockRule {

    override fun validate(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): ValidationResult {
        val allErrors = mutableListOf<String>()

        for (rule in rules) {
            val result = rule.validate(block, difficulty, previousBlock)
            if (!result.isValid) {
                allErrors.addAll(result.errorList)
            }
        }

        return ValidationResult(
            isValid = allErrors.isEmpty(),
            errorList = allErrors,
        )
    }
}
