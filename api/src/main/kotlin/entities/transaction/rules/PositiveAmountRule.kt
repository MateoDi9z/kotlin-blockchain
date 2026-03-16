package api.entities.transaction.rules

import api.dtos.Transaction

class PositiveAmountRule : TransactionRule {
    override fun isValid(transaction: Transaction): Boolean = transaction.amount > 0L

    override fun getErrorMessage() = "The amount of transactions must be positive"
}
