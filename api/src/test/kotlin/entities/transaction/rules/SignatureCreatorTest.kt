package entities.transaction.rules
import api.entities.SignatureManager.createSignature
import api.entities.SignatureManager.getPublicKeyFromString
import api.entities.SignatureManager.verifySignature
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

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

    @Test
    fun `verifySignature returns false for malformed base64 signature`() {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val pair = keyGen.generateKeyPair()

        val isValid = verifySignature(pair.public, "message", "not-valid-base64!!!")

        assertFalse(isValid, "Verification must fail gracefully on malformed signature bytes")
    }

    @Test
    fun `verifySignature returns false for invalid signature bytes`() {
        val keyGen = KeyPairGenerator.getInstance("EC")
        val pair = keyGen.generateKeyPair()
        val garbage = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3, 4))

        val isValid = verifySignature(pair.public, "message", garbage)

        assertFalse(isValid, "Verification must fail gracefully on invalid signature bytes")
    }

    @Test
    fun `verifySignature returns false when publicKey is null`() {
        assertFalse(
            verifySignature(null, "message", "signature"),
            "Verification must fail gracefully on null public key",
        )
    }

    @Test
    fun `getPublicKeyFromString returns null for malformed base64`() {
        val result = getPublicKeyFromString("not-valid-base64!!!")

        assertNull(result, "Should return null on malformed Base64 input")
    }

    @Test
    fun `getPublicKeyFromString returns null for invalid key material`() {
        val garbage = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3, 4))

        val result = getPublicKeyFromString(garbage)

        assertNull(result, "Should return null when key material is invalid")
    }
}
