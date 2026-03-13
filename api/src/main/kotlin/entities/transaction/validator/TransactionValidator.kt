package api.entities.transaction.validator

import api.dtos.Transaction
import api.entities.transaction.rules.TransactionRule

class TransactionValidator(
    private val rules: List<TransactionRule>,
) {
    private val errors = mutableListOf<String>()

    fun validate(transaction: Transaction): Boolean {
        for (rule in rules) {
            if (!rule.isValid(transaction)) {
                errors.add(rule.getErrorMessage())
            }
        }
        return errors.isEmpty()
    }

    fun getErrors(): String = errors.joinToString(" | ")
}
