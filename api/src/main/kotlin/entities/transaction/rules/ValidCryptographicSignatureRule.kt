package api.entities.transaction.rules

import api.dtos.Transaction
import api.entities.getPublicKeyFromString
import api.entities.verifySignature

class ValidCryptographicSignatureRule : TransactionRule {

    override fun isValid(transaction: Transaction): Boolean {
        val publicKey = getPublicKeyFromString(transaction.from) ?: return false
        return verifySignature(
            publicKey = publicKey,
            message = transaction.from + transaction.to + transaction.amount,
            signature = transaction.signature,
        )
    }

    override fun getErrorMessage(): String = "The signature of the transaction is not valid"
}
