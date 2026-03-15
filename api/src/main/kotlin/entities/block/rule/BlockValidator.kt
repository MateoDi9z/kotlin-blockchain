package entities.block.rule

import entities.block.Block
import entities.results.ValidationResult
import entities.transaction.validator.TransactionValidator

class BlockValidator(
    private val rule: BlockRule,
) {
    fun validateReceivedBlock(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
        txValidator: TransactionValidator,
    ): ValidationResult {
        val allErrors = mutableListOf<String>()
        allErrors += collectBlockErrors(block, difficulty, previousBlock)
        allErrors += collectTransactionErrors(block, txValidator)
        return makeValidationResult(allErrors)
    }

    private fun collectBlockErrors(
        block: Block,
        difficulty: Int,
        previousBlock: Block,
    ): List<String> {
        val result = rule.validate(block, difficulty, previousBlock)
        return if (result.isFailure()) result.errorList else emptyList()
    }

    private fun collectTransactionErrors(
        block: Block,
        txValidator: TransactionValidator,
    ): List<String> {
        val errors = mutableListOf<String>()
        block.transactions.forEach { tx ->
            val txResult = txValidator.validate(tx)
            if (txResult.isFailure()) errors.addAll(txResult.errorList)
        }
        return errors
    }

    private fun makeValidationResult(errors: List<String>) =
        ValidationResult(isValid = errors.isEmpty(), errorList = errors)
}
