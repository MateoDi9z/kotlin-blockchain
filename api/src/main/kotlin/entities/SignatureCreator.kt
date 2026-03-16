package api.entities

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

fun createSignature(
    privateKey: PrivateKey,
    message: String,
): String {
    val s = Signature.getInstance("SHA256withECDSA")
    s.initSign(privateKey)
    s.update(message.toByteArray())
    val signatureBytes = s.sign()
    return Base64.getEncoder().encodeToString(signatureBytes)
}

fun verifySignature(
    publicKey: PublicKey?,
    message: String,
    signature: String,
): Boolean {
    if (publicKey == null) return false
    return try {
        val s = Signature.getInstance("SHA256withECDSA")
        s.initVerify(publicKey)
        s.update(message.toByteArray())
        val signatureBytes = Base64.getDecoder().decode(signature)
        s.verify(signatureBytes)
    } catch (e: Exception) {
        false
    }
}

fun getPublicKeyFromString(keyString: String): PublicKey? =
    try {
        val publicBytes = Base64.getDecoder().decode(keyString)
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        keyFactory.generatePublic(keySpec)
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: InvalidKeySpecException) {
        null
    }
