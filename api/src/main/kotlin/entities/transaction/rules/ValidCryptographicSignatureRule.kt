package api.entities.transaction.rules

import api.dtos.Transaction
import api.entities.getPublicKeyFromString
import api.entities.verifySignature

class ValidCryptographicSignatureRule : TransactionRule {

    override fun isValid(transaction: Transaction): Boolean {
        val publicKey = getPublicKeyFromString(transaction.from) ?: return false
        val message =
            """{"from":"${transaction.from}",
            |"to":"${transaction.to}","amount":${transaction.amount}}
            """.trimMargin()
        return verifySignature(
            publicKey = publicKey,
            message = message,
            signature = transaction.signature,
        )
    }

    override fun getErrorMessage(): String = "The signature of the transaction is not valid"
}
