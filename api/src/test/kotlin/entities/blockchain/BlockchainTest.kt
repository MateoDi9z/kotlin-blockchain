package entities.blockchain

import api.entities.blockchain.results.ChainSyncResult
import entities.blockchain.results.TransactionAdditionResult
import api.entities.transaction.rules.PositiveAmountRule
import entities.block.Block
import entities.transaction.validator.TransactionValidator
import testutils.TestBuilders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockchainTest {

    private val testDifficulty = 2
    private val simpleValidator = TransactionValidator(listOf(PositiveAmountRule()))

    @Test
    fun init_createsGenesisBlock() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        assertEquals(1, blockchain.chain.size, "Chain should start with 1 block")

        val genesisBlock = blockchain.chain.first()
        assertEquals(0, genesisBlock.index)
        assertEquals("0", genesisBlock.previousHash)
    }

    @Test
    fun addTransaction_addsValidTransactionToPending() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val validTx = TestBuilders.makeTransaction(amount = 10L)

        val result = blockchain.addTransaction(validTx)

        assertTrue(result is TransactionAdditionResult.Success)
        assertEquals(1, blockchain.pendingTransactions.size)
        assertEquals(validTx, blockchain.pendingTransactions.first())
    }

    @Test
    fun addTransaction_rejectsInvalidTransactionWithErrors() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val invalidTx = TestBuilders.makeTransaction(amount = -5L)

        val result = blockchain.addTransaction(invalidTx)

        assertTrue(result is TransactionAdditionResult.Rejected)
        assertTrue(
            blockchain.pendingTransactions.isEmpty(),
            "Pending transactions list should remain empty",
        )
    }

    @Test
    fun minePendingTransactions_returnsMinedBlockAndClearsPending() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        blockchain.addTransaction(TestBuilders.makeTransaction(from = "A", to = "B", amount = 10L))
        blockchain.addTransaction(TestBuilders.makeTransaction(from = "C", to = "D", amount = 5L))

        val minedBlock = blockchain.minePendingTransactions()

        assertEquals(2, blockchain.chain.size, "Chain should have 2 blocks (Genesis + New)")
        assertTrue(
            blockchain.pendingTransactions.isEmpty(),
            "Pending transactions must be cleared after mining",
        )

        val latestBlock = blockchain.getLatestBlock()
        assertEquals(latestBlock, minedBlock)
        assertEquals(1, latestBlock.index)
        assertEquals(
            2,
            latestBlock.transactions.size,
            "Block must contain the 2 mined transactions",
        )
        assertTrue(latestBlock.hash.startsWith("00"), "Hash must meet the difficulty of 2 zeros")
    }

    @Test
    fun isChainValid_returnsTrueForUntamperedChain() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        blockchain.minePendingTransactions()
        blockchain.addTransaction(TestBuilders.makeTransaction(amount = 50L))
        blockchain.minePendingTransactions()

        assertTrue(
            blockchain.isChainValid(blockchain.chain),
            "A legally mined chain should be considered valid",
        )
    }

    @Test
    fun isChainValid_returnsFalseWhenBlockIsTampered() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        blockchain.minePendingTransactions()

        val tamperedChain = blockchain.chain.toMutableList()
        val originalBlock = tamperedChain[1]

        val tamperedBlock =
            Block(
                index = originalBlock.index,
                timestamp = originalBlock.timestamp,
                transactions = originalBlock.transactions,
                previousHash = originalBlock.previousHash,
                hash = "fake_invented_hash",
                nonce = originalBlock.nonce,
            )
        tamperedChain[1] = tamperedBlock

        assertFalse(
            blockchain.isChainValid(tamperedChain),
            "Chain should be rejected due to tampered hash",
        )
    }

    @Test
    fun isChainValid_returnsFalseWhenChainLinkIsBroken() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        blockchain.minePendingTransactions()

        val tamperedChain = blockchain.chain.toMutableList()
        val originalBlock = tamperedChain[1]

        val unlinkedBlock =
            TestBuilders.makeMinedBlock(
                difficulty = testDifficulty,
                index = originalBlock.index,
                previousHash = "broken_link",
            )

        tamperedChain[1] = unlinkedBlock

        assertFalse(
            blockchain.isChainValid(tamperedChain),
            "Chain should be rejected due to broken previous hash links",
        )
    }

    @Test
    fun replaceChain_replacesLocalChainWithLongerValidChain() {
        val nodeA = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val nodeB = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        nodeB.addTransaction(TestBuilders.makeTransaction(amount = 100L))
        nodeB.minePendingTransactions()

        val result = nodeA.replaceChain(nodeB.chain)

        assertTrue(result is ChainSyncResult.Success)
        assertEquals(2, nodeA.chain.size, "Node A should have adopted Node B's chain")
        assertEquals(nodeB.getLatestBlock().hash, nodeA.getLatestBlock().hash)
    }

    @Test
    fun replaceChain_rejectsShorterChain() {
        val nodeA = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val nodeB = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        nodeA.minePendingTransactions()
        nodeA.minePendingTransactions()

        nodeB.minePendingTransactions()

        val result = nodeA.replaceChain(nodeB.chain)

        assertTrue(result is ChainSyncResult.Rejected)
        assertEquals(
            "Received chain is shorter or equal to the local chain.",
            (result as ChainSyncResult.Rejected).reason,
        )
        assertEquals(3, nodeA.chain.size, "Node A should keep its chain of 3 blocks")
    }
}
