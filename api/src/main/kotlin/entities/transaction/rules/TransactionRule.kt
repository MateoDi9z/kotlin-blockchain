package api.entities.transaction.rules

import api.dtos.Transaction

interface TransactionRule {
    fun isValid(transaction: Transaction): Boolean

    fun getErrorMessage(): String
}
