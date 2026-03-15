package entities.blockchain

import api.dtos.Transaction
import entities.block.Block
import entities.block.BlockMiner
import entities.results.OperationResult

class Blockchain(
    val difficulty: Int,
    private val transactionPool: TransactionPool = TransactionPool(),
    private val ledger: Chain = Chain(difficulty),
    private val miner: BlockMiner = BlockMiner,
) {

    val chain: List<Block> get() = ledger.blocks
    val pendingTransactions: List<Transaction> get() = transactionPool.pendingTransactions

    fun addTransaction(transaction: Transaction): OperationResult<Unit> =
        transactionPool.addTransaction(transaction)

    fun minePendingTransactions(): Block {
        val transactionsToMine = transactionPool.extractTransactionsForMining()
        val latestBlock = ledger.getLatestBlock()

        val unminedBlock =
            Block(
                index = latestBlock.index + 1,
                timestamp = System.currentTimeMillis(),
                transactions = transactionsToMine,
                previousHash = latestBlock.hash,
            )
        val minedBlock = miner.mine(unminedBlock, difficulty)

        ledger.addBlock(minedBlock)

        return minedBlock
    }

    fun getLatestBlock(): Block = ledger.getLatestBlock()

    fun replaceChain(newChain: List<Block>): OperationResult<Unit> = ledger.replaceChain(newChain)
}
