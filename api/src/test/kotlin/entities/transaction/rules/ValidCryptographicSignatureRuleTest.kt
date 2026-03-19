package entities.transaction.rules

import api.dtos.Transaction
import api.entities.createSignature
import api.entities.transaction.rules.ValidCryptographicSignatureRule
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidCryptographicSignatureRuleTest {

    private lateinit var rule: ValidCryptographicSignatureRule
    private lateinit var keyPair: KeyPair
    private lateinit var publicKeyString: String

    @BeforeTest
    fun setup() {
        rule = ValidCryptographicSignatureRule()
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyPair = keyGen.generateKeyPair()
        publicKeyString = Base64.getEncoder().encodeToString(keyPair.public.encoded)
    }

    @Test
    fun `True for a correctly signed transaction`() {
        val from = publicKeyString
        val to = "recipient-address"
        val amount = 100L
        val message = """{"from":"$from","to":"$to","amount":$amount}"""

        val signature = createSignature(keyPair.private, message)
        val tx = Transaction(from, to, amount, signature)

        assertTrue(rule.isValid(tx), "The signature should be valid for correct data")
    }

    @Test
    fun `False if data was tampered after signing`() {
        val from = publicKeyString
        val message = """{"from":"$from","to":"bob","amount":${10}}"""
        val signature = createSignature(keyPair.private, message)

        val tamperedTx = Transaction(from, "bob", 500L, signature)

        assertFalse(rule.isValid(tamperedTx), "The rule should catch tampered amounts")
    }

    @Test
    fun `False for a signature from a different private key`() {
        val otherKeyPair = KeyPairGenerator.getInstance("EC").generateKeyPair()
        val message = """{"from":"$publicKeyString","to":"bob","amount":${10}}"""

        val invalidSignature = createSignature(otherKeyPair.private, message)
        val tx = Transaction(publicKeyString, "bob", 10L, invalidSignature)

        assertFalse(rule.isValid(tx), "The signature should not be valid for this public key")
    }
}
