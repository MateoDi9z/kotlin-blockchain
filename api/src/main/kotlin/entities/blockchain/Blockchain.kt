package entities.blockchain

import api.dtos.Transaction
import entities.block.Block
import entities.results.OperationResult

class Blockchain(
    val difficulty: Int,
    private val transactionPool: TransactionPool = TransactionPool(),
    private val ledger: Chain = Chain(difficulty),
) {

    val chain: List<Block> get() = ledger.blocks
    val pendingTransactions: List<Transaction> get() = transactionPool.pendingTransactions

    fun addTransaction(transaction: Transaction): OperationResult<Unit> =
        transactionPool.addTransaction(transaction)

    fun getLatestBlock(): Block = ledger.getLatestBlock()

    fun getPendingTransactions(): List<Transaction> = transactionPool.extractTransactionsForMining()

    fun addBlock(block: Block): OperationResult<Unit> = ledger.addBlock(block)

    fun replaceChain(newChain: List<Block>): OperationResult<Unit> = ledger.replaceChain(newChain)
}
