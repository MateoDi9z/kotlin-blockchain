package api.entities.node

import api.configuration.NodeIdentityProperties
import api.entities.SignatureManager.createSignature
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Component
class NodeIdentity(
    identityProps: NodeIdentityProperties,
) {
    lateinit var publicKey: PublicKey
    private val privateKey: PrivateKey

    val publicUrl: String = identityProps.publicUrl

    init {
        val keyFactory = KeyFactory.getInstance("EC")

        try {
            val cleanPub = cleanKeyString(identityProps.publicKey)
            val cleanPriv = cleanKeyString(identityProps.privateKey)

            val publicBytes = Base64.getDecoder().decode(cleanPub)
            val privateBytes = Base64.getDecoder().decode(cleanPriv)

            this.publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
            this.privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))
        } catch (e: Exception) {
            throw IllegalStateException(
                "CRITICAL: Failed to parse cryptographic keys from application properties. " +
                    "Ensure you generated Elliptic Curve (EC) keys and the private key is in PKCS#8 format.",
                e,
            )
        }
    }

    fun signMessage(message: String): String = createSignature(privateKey, message)

    fun getPublicKeyBase64(): String = Base64.getEncoder().encodeToString(publicKey.encoded)

    private fun cleanKeyString(key: String): String =
        key
            .replace("-----BEGIN.*?-----".toRegex(), "")
            .replace("-----END.*?-----".toRegex(), "")
            .replace("\\s".toRegex(), "")
}
