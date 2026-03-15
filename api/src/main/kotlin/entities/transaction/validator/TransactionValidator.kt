package entities.transaction.validator

import api.dtos.Transaction
import api.entities.transaction.rules.TransactionRule
import entities.results.ValidationResult

class TransactionValidator(
    private val rules: List<TransactionRule>,
) {
    fun validate(transaction: Transaction): ValidationResult {
        val localErrors = mutableListOf<String>()

        for (rule in rules) {
            if (!rule.isValid(transaction)) {
                localErrors.add(rule.getErrorMessage())
            }
        }

        return ValidationResult(
            isValid = localErrors.isEmpty(),
            errorList = localErrors,
        )
    }
}
