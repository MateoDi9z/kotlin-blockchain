package entities.blockchain

import entities.block.BlockMiner
import entities.results.OperationResult
import entities.transaction.validator.TransactionValidator
import api.entities.transaction.rules.PositiveAmountRule
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

        val minedBlock = blockchain.minePendingTransactions()

        assertEquals(2, blockchain.chain.size, "Chain should have genesis + 1 new block")
        assertTrue(blockchain.pendingTransactions.isEmpty(), "Mempool should be empty after mining")
        assertEquals(
            2,
            minedBlock.transactions.size,
            "Block should contain the 2 pending transactions",
        )
        assertTrue(minedBlock.hash.startsWith("00"), "Block should meet the PoW difficulty")
    }
}
