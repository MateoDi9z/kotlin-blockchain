package api.entities.transaction.rules

import api.dtos.Transaction

class SignatureNotEmptyRule : TransactionRule {
    override fun isValid(transaction: Transaction): Boolean = transaction.signature.isNotEmpty()

    override fun getErrorMessage(): String = "Signature is required for a transaction"
}
