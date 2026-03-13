package api.entities.transaction.rules

import api.dtos.Transaction

class CryptographicSignatureRule : TransactionRule {
    override fun isValid(transaction: Transaction): Boolean {
        TODO("Not yet implemented")
    }

    override fun getErrorMessage(): String {
        TODO("Not yet implemented")
    }
}
