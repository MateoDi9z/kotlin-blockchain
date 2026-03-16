package entities.blockchain

import entities.block.Block
import entities.block.rule.BlockValidator
import entities.block.rule.BlockValidatorFactory
import entities.results.OperationResult
import entities.transaction.validator.TransactionValidator
import entities.transaction.validator.TransactionValidatorFactory

class Chain(
    val difficulty: Int,
    private val blockValidator: BlockValidator = BlockValidatorFactory.createDefault(),
    private val txValidator: TransactionValidator = TransactionValidatorFactory.createDefault(),
) {
    private val _blocks = mutableListOf<Block>()
    val blocks: List<Block> get() = _blocks.toList()

    init {
        if (_blocks.isEmpty()) _blocks.add(genesisBlock())
    }

    private fun genesisBlock(): Block =
        Block(
            index = 0,
            timestamp = 1700000000L,
            transactions = emptyList(),
            previousHash = "0",
            hash = "0xGenesisHashInventadoOPrecalculado",
            nonce = 0,
        )

    fun getLatestBlock(): Block = _blocks.last()

    fun addBlock(block: Block): OperationResult<Unit> {
        val validation =
            blockValidator.validateReceivedBlock(
                block = block,
                difficulty = difficulty,
                previousBlock = getLatestBlock(),
                txValidator = txValidator,
            )
        return if (validation.isValid) {
            addBlockInternal(block)
        } else {
            OperationResult.Failure(validation.errorList)
        }
    }

    private fun addBlockInternal(block: Block): OperationResult<Unit> {
        _blocks.add(block)
        return OperationResult.Success(Unit)
    }

    fun isChainValid(chainToValidate: List<Block>): Boolean {
        for (i in 1 until chainToValidate.size) {
            if (!isBlockValidForPrevious(chainToValidate[i], chainToValidate[i - 1])) return false
        }
        return true
    }

    private fun isBlockValidForPrevious(
        block: Block,
        previous: Block,
    ): Boolean {
        val validation =
            blockValidator.validateReceivedBlock(
                block = block,
                difficulty = difficulty,
                previousBlock = previous,
                txValidator = txValidator,
            )
        return validation.isValid
    }

    fun replaceChain(newChain: List<Block>): OperationResult<Unit> {
        if (newChain.size <= _blocks.size) {
            return OperationResult.Failure(
                listOf("Received chain is shorter or equal to the local chain."),
            )
        }

        if (newChain.first() != _blocks.first()) {
            return OperationResult.Failure(
                listOf("Received chain has a different genesis block."),
            )
        }

        if (!isChainValid(newChain)) {
            return OperationResult.Failure(
                listOf("Received chain contains invalid blocks or broken links."),
            )
        }

        _blocks.clear()
        _blocks.addAll(newChain)
        return OperationResult.Success(Unit)
    }
}
