package api.entities.transaction.rules

import api.dtos.Transaction
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class TransactionRulesTest {

    @Test
    fun amountRule_returnsTrueForPositiveAmounts() {
        val rule = PositiveAmountRule()
        val tx = Transaction(from = "alice", to = "bob", amount = 10.0f, signature = "sig")

        assertTrue(rule.isValid(tx))
    }

    @Test
    fun positiveAmountRule_returnsFalseForZeroOrNegativeAmounts() {
        val rule = PositiveAmountRule()
        val txZero = Transaction(from = "a", to = "b", amount = 0.0f, signature = "s")
        val txNegative = Transaction(from = "a", to = "b", amount = -5.0f, signature = "s")

        assertFalse(rule.isValid(txZero))
        assertFalse(rule.isValid(txNegative))
    }

    @Test
    fun signatureNotEmptyRule_returnsTrueForNonEmptySignature() {
        val rule = SignatureNotEmptyRule()
        val tx = Transaction(from = "a", to = "b", amount = 1.0f, signature = "non-empty")

        assertTrue(rule.isValid(tx))
    }

    @Test
    fun signatureNotEmptyRule_returnsFalseForEmptySignature() {
        val rule = SignatureNotEmptyRule()
        val tx = Transaction(from = "a", to = "b", amount = 1.0f, signature = "")

        assertFalse(rule.isValid(tx))
    }

    @Test
    fun compositeTransactionRule_returnsTrueAndNoErrorsWhenAllRulesPass() {
        val rules = listOf(PositiveAmountRule(), SignatureNotEmptyRule())
        val composite = CompositeTransactionRule(rules)
        val tx = Transaction(from = "a", to = "b", amount = 2.5f, signature = "sig")

        assertTrue(composite.isValid(tx))
        assertEquals("No errors found", composite.getErrorMessage())
        assertEquals(emptyList<String>(), composite.getErrorList())
    }

    @Test
    fun compositeTransactionRule_collectsErrorsAndReturnsFalseWhenRulesFail() {
        val rules = listOf(PositiveAmountRule(), SignatureNotEmptyRule())
        val composite = CompositeTransactionRule(rules)
        val tx = Transaction(from = "a", to = "b", amount = 0.0f, signature = "")

        assertFalse(composite.isValid(tx))

        val expectedErrors =
            listOf(
                "The amount of transactions must be positive",
                "Signature is required for a transaction",
            )

        assertEquals(expectedErrors, composite.getErrorList())
        assertEquals(expectedErrors.joinToString(" | "), composite.getErrorMessage())
    }

    @Test
    fun transactionValidator_returnsTrueAndEmptyErrorsWhenAllRulesPass() {
        val rules = listOf(PositiveAmountRule(), SignatureNotEmptyRule())
        val validator =
            api.entities.transaction.validator
                .TransactionValidator(rules)
        val tx = Transaction(from = "a", to = "b", amount = 3.0f, signature = "sig")

        assertTrue(validator.validate(tx))
        assertEquals("", validator.getErrors())
    }

    @Test
    fun transactionValidator_returnsFalseAndAggregatesErrorsWhenRulesFail() {
        val rules = listOf(PositiveAmountRule(), SignatureNotEmptyRule())
        val validator =
            api.entities.transaction.validator
                .TransactionValidator(rules)
        val tx = Transaction(from = "a", to = "b", amount = 0.0f, signature = "")

        assertFalse(validator.validate(tx))

        val expected =
            listOf(
                "The amount of transactions must be positive",
                "Signature is required for a transaction",
            ).joinToString(" | ")

        assertEquals(expected, validator.getErrors())
    }
}
