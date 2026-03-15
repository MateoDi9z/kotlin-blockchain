package entities.transaction.rules

import api.dtos.Transaction
import api.entities.transaction.rules.PositiveAmountRule
import api.entities.transaction.rules.SignatureNotEmptyRule
import entities.transaction.validator.TransactionValidator
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class TransactionRulesTest {

    @Test
    fun amountRule_returnsTrueForPositiveAmounts() {
        val rule = PositiveAmountRule()
        val tx = Transaction(from = "alice", to = "bob", amount = 10L, signature = "sig")

        assertTrue(rule.isValid(tx))
    }

    @Test
    fun positiveAmountRule_returnsFalseForZeroOrNegativeAmounts() {
        val rule = PositiveAmountRule()
        val txZero = Transaction(from = "a", to = "b", amount = 0L, signature = "s")
        val txNegative = Transaction(from = "a", to = "b", amount = -5L, signature = "s")

        assertFalse(rule.isValid(txZero))
        assertFalse(rule.isValid(txNegative))
    }

    @Test
    fun signatureNotEmptyRule_returnsTrueForNonEmptySignature() {
        val rule = SignatureNotEmptyRule()
        val tx = Transaction(from = "a", to = "b", amount = 1L, signature = "non-empty")

        assertTrue(rule.isValid(tx))
    }

    @Test
    fun signatureNotEmptyRule_returnsFalseForEmptySignature() {
        val rule = SignatureNotEmptyRule()
        val tx = Transaction(from = "a", to = "b", amount = 1L, signature = "")

        assertFalse(rule.isValid(tx))
    }

    @Test
    fun transactionValidator_returnsTrueAndEmptyErrorsWhenAllRulesPass() {
        val rules = listOf(PositiveAmountRule(), SignatureNotEmptyRule())
        val validator = TransactionValidator(rules)
        val tx = Transaction(from = "a", to = "b", amount = 3L, signature = "sig")

        val result = validator.validate(tx)

        assertTrue(result.isValid)
        assertEquals("No errors found", result.getErrors())
    }

    @Test
    fun transactionValidator_returnsFalseAndAggregatesErrorsWhenRulesFail() {
        val rules = listOf(PositiveAmountRule(), SignatureNotEmptyRule())
        val validator = TransactionValidator(rules)
        val tx = Transaction(from = "a", to = "b", amount = 0L, signature = "")

        val result = validator.validate(tx)

        assertFalse(result.isValid)

        val expected =
            listOf(
                "The amount of transactions must be positive",
                "Signature is required for a transaction",
            ).joinToString(" | ")

        assertEquals(expected, result.getErrors())
    }
}
