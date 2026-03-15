package entities.transaction.rules
import api.entities.createSignature
import api.entities.getPublicKeyFromString
import api.entities.verifySignature
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SignatureCreatorTest {

    @Test
    fun `create and verify signature should work with valid data`() {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val pair = keyGen.generateKeyPair()
        val message = "blockchain-transaction-data"

        val signature = createSignature(pair.private, message)
        val isValid = verifySignature(pair.public, message, signature)

        assertTrue(isValid, "Standard signature flow should pass")
    }

    @Test
    fun `verifySignature returns false for modified message`() {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val pair = keyGen.generateKeyPair()

        val signature = createSignature(pair.private, "original")
        val isValid = verifySignature(pair.public, "modified", signature)

        assertFalse(isValid, "Verification must fail if the message changes")
    }

    @Test
    fun `getPublicKeyFromString reconstructs a valid public key`() {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val pair = keyGen.generateKeyPair()
        val encoded = Base64.getEncoder().encodeToString(pair.public.encoded)

        val reconstructedKey = getPublicKeyFromString(encoded)

        val signature = createSignature(pair.private, "test")
        assertTrue(verifySignature(reconstructedKey, "test", signature))
    }
}
