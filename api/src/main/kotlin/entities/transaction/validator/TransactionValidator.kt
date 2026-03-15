package api.entities.transaction.validator

import api.dtos.Transaction
import api.entities.transaction.rules.TransactionRule

class TransactionValidator(
    private val rules: List<TransactionRule>,
) {
    private val errors = mutableListOf<String>()

    fun validate(transaction: Transaction): Boolean {
        errors.clear()

        for (rule in rules) {
            if (!rule.isValid(transaction)) {
                errors.add(rule.getErrorMessage())
            }
        }

        return errors.isEmpty()
    }

    fun getErrors(): String =
        if (errors.isNotEmpty()) {
            errors.joinToString(separator = " | ")
        } else {
            "No errors found"
        }

    fun getErrorList(): List<String> = errors.toList()
}