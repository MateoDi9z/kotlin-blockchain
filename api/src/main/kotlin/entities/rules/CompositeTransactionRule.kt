package api.entities.rules

import api.entities.Transaction

class CompositeTransactionRule(
    private val rules: List<TransactionRule>
) : TransactionRule {

    private val errors = mutableListOf<String>()

    override fun isValid(transaction: Transaction): Boolean {
        errors.clear()

        for (rule in rules) {
            if (!rule.isValid(transaction)) {
                errors.add(rule.getErrorMessage())
            }
        }

        return errors.isEmpty()
    }

    override fun getErrorMessage(): String {
        return if (errors.isNotEmpty()) {
            errors.joinToString(separator = " | ")
        } else {
            "No errors found"
        }
    }

    fun getErrorList(): List<String> = errors.toList()
}