package entities.blockchain

import entities.Blockchain
import api.entities.transaction.rules.PositiveAmountRule
import entities.block.Block
import entities.transaction.validator.TransactionValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import testutils.TestBuilders

class BlockchainTest {

    private val testDifficulty = 2

    private val simpleValidator = TransactionValidator(listOf(PositiveAmountRule()))

    @Test
    fun init_createsGenesisBlock() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        assertEquals(1, blockchain.chain.size, "La cadena debe iniciar con 1 bloque")

        val genesisBlock = blockchain.chain.first()
        assertEquals(0, genesisBlock.index)
        assertEquals("0", genesisBlock.previousHash)
    }

    @Test
    fun addTransaction_addsValidTransactionToPending() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val validTx = TestBuilders.makeTransaction(amount = 10L)

        val result = blockchain.addTransaction(validTx)

        assertTrue(result)
        assertEquals(1, blockchain.pendingTransactions.size)
        assertEquals(validTx, blockchain.pendingTransactions.first())
    }

    @Test
    fun addTransaction_rejectsInvalidTransaction() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val invalidTx = TestBuilders.makeTransaction(amount = -5L)

        val result = blockchain.addTransaction(invalidTx)

        assertFalse(result)
        assertTrue(
            blockchain.pendingTransactions.isEmpty(),
            "No debe agregar la transacción a la lista",
        )
    }

    @Test
    fun minePendingTransactions_createsNewBlockAndClearsPending() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        blockchain.addTransaction(TestBuilders.makeTransaction(from = "A", to = "B", amount = 10L))
        blockchain.addTransaction(TestBuilders.makeTransaction(from = "C", to = "D", amount = 5L))

        blockchain.minePendingTransactions()

        assertEquals(2, blockchain.chain.size, "Debería haber 2 bloques (Génesis + Nuevo)")
        assertTrue(
            blockchain.pendingTransactions.isEmpty(),
            "Las transacciones pendientes deben vaciarse",
        )

        val latestBlock = blockchain.getLatestBlock()
        assertEquals(1, latestBlock.index)
        assertEquals(
            2,
            latestBlock.transactions.size,
            "El bloque debe contener las 2 transacciones",
        )
        assertTrue(
            latestBlock.hash.startsWith("00"),
            "El hash debe cumplir con la dificultad de 2 ceros",
        )
    }

    @Test
    fun isChainValid_returnsTrueForUntamperedChain() {
        val blockchain = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        blockchain.minePendingTransactions()
        blockchain.addTransaction(TestBuilders.makeTransaction(amount = 50L))
        blockchain.minePendingTransactions()

        assertTrue(
            blockchain.isChainValid(blockchain.chain),
            "Una cadena minada legalmente debe ser válida",
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
                hash = "hash_falso_inventado",
                nonce = originalBlock.nonce,
            )
        tamperedChain[1] = tamperedBlock

        assertFalse(
            blockchain.isChainValid(tamperedChain),
            "La cadena debe ser rechazada porque el hash fue manipulado",
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
                previousHash = "enlace_roto",
            )

        tamperedChain[1] = unlinkedBlock

        assertFalse(
            blockchain.isChainValid(tamperedChain),
            "La cadena debe ser rechazada porque los bloques no están encadenados correctamente",
        )
    }

    @Test
    fun replaceChain_replacesLocalChainWithLongerValidChain() {
        val nodeA = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val nodeB = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        nodeB.addTransaction(TestBuilders.makeTransaction(amount = 100L))
        nodeB.minePendingTransactions()

        nodeA.replaceChain(nodeB.chain)

        assertEquals(2, nodeA.chain.size, "El Nodo A debería haber adoptado la cadena del Nodo B")
        assertEquals(nodeB.getLatestBlock().hash, nodeA.getLatestBlock().hash)
    }

    @Test
    fun replaceChain_rejectsShorterChain() {
        val nodeA = Blockchain(difficulty = testDifficulty, validator = simpleValidator)
        val nodeB = Blockchain(difficulty = testDifficulty, validator = simpleValidator)

        nodeA.minePendingTransactions()
        nodeA.minePendingTransactions()

        nodeB.minePendingTransactions()

        nodeA.replaceChain(nodeB.chain)

        assertEquals(
            3,
            nodeA.chain.size,
            "El Nodo A debe conservar su cadena de 3 bloques (Génesis + 2)",
        )
    }
}
