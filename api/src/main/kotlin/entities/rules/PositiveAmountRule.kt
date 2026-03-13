package api.entities.rules

import api.entities.Transaction

class PositiveAmountRule : TransactionRule {
    override fun isValid(transaction: Transaction): Boolean {
        return transaction.amount > 0.0
    }

    override fun getErrorMessage() = "The amount of transactions must be positive"
}