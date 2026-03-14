package api.entities.transaction.rules

import api.dtos.Transaction
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

class ValidCryptographicSignatureRule : TransactionRule {

    override fun isValid(transaction: Transaction): Boolean =
        verifySignature(
            publicKey = getPublicKeyFromString(transaction.to),
            message = transaction.from + transaction.to + transaction.amount,
            signature = transaction.signature,
        )

    override fun getErrorMessage(): String {
        TODO("Not yet implemented")
    }

    fun verifySignature(
        publicKey: PublicKey,
        message: String,
        signature: String,
    ): Boolean {
        val s = Signature.getInstance("SHA256withRSA")
        s.initVerify(publicKey)
        s.update(message.toByteArray())
        val signatureBytes = Base64.getDecoder().decode(signature)
        return s.verify(signatureBytes)
    }

    private fun getPublicKeyFromString(keyString: String): PublicKey {
        TODO("Implementar conversión de String Base64/Hex a objeto PublicKey de Java")
    }
}
