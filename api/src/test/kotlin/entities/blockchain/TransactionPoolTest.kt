package entities.blockchain

import api.entities.transaction.rules.PositiveAmountRule
import entities.results.OperationResult
import entities.transaction.validator.TransactionValidator
import testutils.TestBuilders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionPoolTest {

    private val simpleValidator = TransactionValidator(listOf(PositiveAmountRule()))

    @Test
    fun addTransaction_whenValid_addsToPoolAndReturnsSuccess() {
        val pool = TransactionPool(txValidator = simpleValidator)
        val validTx = TestBuilders.makeTransaction(amount = 100L)

        val result = pool.addTransaction(validTx)

        assertTrue(result is OperationResult.Success, "Result should be Success")
        assertEquals(1, pool.pendingTransactions.size, "Pool should contain 1 transaction")
        assertEquals(validTx, pool.pendingTransactions.first())
    }

    @Test
    fun addTransaction_whenInvalid_rejectsAndReturnsFailure() {
        val pool = TransactionPool(txValidator = simpleValidator)
        val invalidTx = TestBuilders.makeTransaction(amount = -50L)

        val result = pool.addTransaction(invalidTx)

        assertTrue(result is OperationResult.Failure, "Result should be Failure")
        assertTrue(pool.pendingTransactions.isEmpty(), "Pool should remain empty")
    }

    @Test
    fun extractTransactionsForMining_returnsPendingAndClearsPool() {
        val pool = TransactionPool(txValidator = simpleValidator)
        pool.addTransaction(TestBuilders.makeTransaction(amount = 10L))
        pool.addTransaction(TestBuilders.makeTransaction(amount = 20L))

        val extracted = pool.extractTransactionsForMining()

        assertEquals(2, extracted.size, "Should extract exactly 2 transactions")
        assertTrue(pool.pendingTransactions.isEmpty(), "Pool must be empty after extraction")
    }
}
