package entities.blockchain

import entities.block.Block
import entities.block.BlockMiner
import entities.results.OperationResult
import entities.transaction.validator.TransactionValidator
import api.entities.transaction.rules.PositiveAmountRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChainTest {

    private val testDifficulty = 2
    private val miner = BlockMiner
    private val txValidator = TransactionValidator(listOf(PositiveAmountRule()))

    @Test
    fun init_createsGenesisBlock() {
        val chain = Chain(difficulty = testDifficulty, txValidator = txValidator)

        assertEquals(1, chain.blocks.size, "Chain should start with genesis block")
        assertEquals(0, chain.blocks.first().index)
    }

    @Test
    fun addBlock_whenValid_appendsToChainAndReturnsSuccess() {
        val chain = Chain(difficulty = testDifficulty, txValidator = txValidator)
        val genesis = chain.getLatestBlock()

        val unminedBlock =
            Block(
                index = genesis.index + 1,
                timestamp = genesis.timestamp + 1000L,
                transactions = emptyList(),
                previousHash = genesis.hash,
            )
        val minedBlock = miner.mine(unminedBlock, testDifficulty)

        val result = chain.addBlock(minedBlock)

        assertTrue(result is OperationResult.Success, "Valid block should be accepted")
        assertEquals(2, chain.blocks.size, "Chain length should be 2")
    }

    @Test
    fun replaceChain_whenNewChainIsLongerAndValid_replacesLocalChain() {
        val localChain = Chain(difficulty = testDifficulty, txValidator = txValidator)
        val remoteChain = Chain(difficulty = testDifficulty, txValidator = txValidator)
        val genesis = remoteChain.getLatestBlock()
        val newBlock =
            miner.mine(
                Block(genesis.index + 1, genesis.timestamp + 1000L, emptyList(), genesis.hash),
                testDifficulty,
            )
        remoteChain.addBlock(newBlock)

        val result = localChain.replaceChain(remoteChain.blocks)

        assertTrue(result is OperationResult.Success, "Longer valid chain should replace local")
        assertEquals(2, localChain.blocks.size)
    }

    @Test
    fun replaceChain_whenNewChainIsShorter_rejectsReplacement() {
        val localChain = Chain(difficulty = testDifficulty, txValidator = txValidator)
        val remoteChain = Chain(difficulty = testDifficulty, txValidator = txValidator)

        val genesis = localChain.getLatestBlock()
        val newBlock =
            miner.mine(
                Block(genesis.index + 1, genesis.timestamp + 1000L, emptyList(), genesis.hash),
                testDifficulty,
            )
        localChain.addBlock(newBlock)

        val result = localChain.replaceChain(remoteChain.blocks)

        assertTrue(result is OperationResult.Failure, "Shorter chain should be rejected")
        assertTrue((result).errors.any { it.contains("shorter") })
        assertEquals(2, localChain.blocks.size, "Local chain should remain intact")
    }
}
