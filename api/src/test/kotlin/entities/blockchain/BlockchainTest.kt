package entities.blockchain

import entities.block.Block
import entities.block.BlockMiner
import entities.block.rule.BlockRule
import entities.block.rule.BlockValidator
import entities.results.OperationResult
import entities.results.ValidationResult
import entities.transaction.validator.TransactionValidator
import entities.transaction.rules.PositiveAmountRule
import testutils.TestBuilders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockchainTest {

    private val testDifficulty = 2
    private val txValidator = TransactionValidator(listOf(PositiveAmountRule()))

    private fun createTestBlockchain(): Blockchain {
        val pool = TransactionPool(txValidator)
        val ledger = Chain(testDifficulty, txValidator = txValidator)
        return Blockchain(testDifficulty, pool, ledger, BlockMiner)
    }

    @Test
    fun addTransaction_delegatesToPool() {
        val blockchain = createTestBlockchain()
        val validTx = TestBuilders.makeTransaction(amount = 50L)

        val result = blockchain.addTransaction(validTx)

        assertTrue(result is OperationResult.Success)
        assertEquals(
            1,
            blockchain.pendingTransactions.size,
            "Transaction should be in pending list",
        )
    }

    @Test
    fun minePendingTransactions_extractsFromPoolMinesAndAddsToLedger() {
        val blockchain = createTestBlockchain()
        blockchain.addTransaction(TestBuilders.makeTransaction(amount = 100L))
        blockchain.addTransaction(TestBuilders.makeTransaction(amount = 200L))

        Thread.sleep(10)

        val result = blockchain.minePendingTransactions()

        assertTrue(result is OperationResult.Success, "Mining should succeed")
        val minedBlock = result.data
        assertEquals(2, blockchain.chain.size, "Chain should have genesis + 1 new block")
        assertTrue(blockchain.pendingTransactions.isEmpty(), "Mempool should be empty after mining")
        assertEquals(
            2,
            minedBlock.transactions.size,
            "Block should contain the 2 pending transactions",
        )
        assertTrue(minedBlock.hash.startsWith("00"), "Block should meet the PoW difficulty")
    }

    @Test
    fun minePendingTransactions_whenAddBlockFails_preservesTransactionsAndReturnsFailure() {
        val alwaysFailRule =
            object : BlockRule {
                override fun validate(
                    block: Block,
                    difficulty: Int,
                    previousBlock: Block,
                ): ValidationResult = ValidationResult(isValid = false, errorList = listOf("simulated validation failure"))
            }
        val failingValidator = BlockValidator(alwaysFailRule)
        val pool = TransactionPool(txValidator)
        val ledger = Chain(testDifficulty, blockValidator = failingValidator, txValidator = txValidator)
        val blockchain = Blockchain(testDifficulty, pool, ledger, BlockMiner)

        blockchain.addTransaction(TestBuilders.makeTransaction(amount = 100L))
        blockchain.addTransaction(TestBuilders.makeTransaction(amount = 200L))

        Thread.sleep(10)

        val result = blockchain.minePendingTransactions()

        assertTrue(result is OperationResult.Failure, "Mining should return Failure when addBlock fails")
        assertEquals(2, blockchain.pendingTransactions.size, "Transactions should remain in pool on failure")
        assertEquals(1, blockchain.chain.size, "Chain should only contain the genesis block")
    }
}
