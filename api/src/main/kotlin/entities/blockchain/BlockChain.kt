package entities.blockchain

import api.dtos.Transaction
import entities.blockchain.results.TransactionAdditionResult
import api.entities.transaction.rules.PositiveAmountRule
import api.entities.transaction.rules.SignatureNotEmptyRule
import api.entities.transaction.rules.ValidCryptographicSignatureRule
import entities.block.Block
import entities.transaction.validator.TransactionValidator

class Blockchain(
    val difficulty: Int,
    private val validator: TransactionValidator =
        TransactionValidator(
            listOf(
                PositiveAmountRule(),
                SignatureNotEmptyRule(),
                ValidCryptographicSignatureRule(),
            ),
        ),
) {
    private val _chain = mutableListOf<Block>()
    val chain: List<Block> get() = _chain.toList()

    private val _pendingTransactions = mutableListOf<Transaction>()
    val pendingTransactions: List<Transaction> get() = _pendingTransactions.toList()

    init {
        if (_chain.isEmpty()) {
            _chain.add(createGenesisBlock())
        }
    }

    private fun createGenesisBlock(): Block =
        Block(
            index = 0,
            timestamp = 1700000000L,
            transactions = emptyList(),
            previousHash = "0",
            hash = "0xGenesisHashInventadoOPrecalculado",
            nonce = 0,
        )

    fun addTransaction(transaction: Transaction): TransactionAdditionResult {
        val result = validator.validate(transaction)

        return if (result.isValid) {
            _pendingTransactions.add(transaction)
            TransactionAdditionResult.Success
        } else {
            TransactionAdditionResult.Rejected(result.getErrors())
        }
    }

    fun getLatestBlock(): Block = _chain.last()

    fun minePendingTransactions(): Block {
        val unminedBlock =
            Block(
                index = getLatestBlock().index + 1,
                timestamp = System.currentTimeMillis(),
                transactions = _pendingTransactions.toList(),
                previousHash = getLatestBlock().hash,
            )

        val minedBlock = unminedBlock.mineBlock(difficulty)

        _chain.add(minedBlock)
        _pendingTransactions.clear()

        return minedBlock
    }

    fun isChainValid(chainToValidate: List<Block>): Boolean {
        for (i in 1 until chainToValidate.size) {
            val currentBlock = chainToValidate[i]
            val previousBlock = chainToValidate[i - 1]

            if (!currentBlock.isValid(difficulty)) {
                return false
            }

            if (currentBlock.previousHash != previousBlock.hash) {
                return false
            }
        }
        return true
    }

    fun replaceChain(newChain: List<Block>): ChainSyncResult {
        if (newChain.size <= _chain.size) {
            return ChainSyncResult.Rejected(
                "Received chain is shorter or equal to the local chain.",
            )
        }

        if (!isChainValid(newChain)) {
            return ChainSyncResult.Rejected(
                "Received chain contains invalid blocks or broken links.",
            )
        }

        _chain.clear()
        _chain.addAll(newChain)
        return ChainSyncResult.Success
    }
}
