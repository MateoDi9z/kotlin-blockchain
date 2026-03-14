package api.entities

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

fun createSignature(
    privateKey: PrivateKey,
    message: String,
): String {
    val s = Signature.getInstance("SHA256withRSA")
    s.initSign(privateKey)
    s.update(message.toByteArray())
    val signatureBytes = s.sign()
    return Base64.getEncoder().encodeToString(signatureBytes)
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
