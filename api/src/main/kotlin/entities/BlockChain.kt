package api.entities
import api.entities.transaction.validator.TransactionValidator
import api.dtos.Transaction
import api.entities.transaction.rules.PositiveAmountRule
import api.entities.transaction.rules.SignatureNotEmptyRule
import api.entities.transaction.rules.ValidCryptographicSignatureRule


class Blockchain(
    val chain: MutableList<Block> = mutableListOf(),
    val difficulty: Int,
    val pendingTransactions: MutableList<Transaction> = mutableListOf(),
    private val validator: TransactionValidator = TransactionValidator(listOf(PositiveAmountRule(), SignatureNotEmptyRule(), ValidCryptographicSignatureRule()))

) {
    init {
        if (chain.isEmpty()) {
            val genesisBlock = createGenesisBlock()
            chain.add(genesisBlock)
        }
    }

    private fun createGenesisBlock(): Block {
        return Block(
            index = 0,
            timestamp = 1700000000L,
            transactions = emptyList(),
            previousHash = "0",
            hash = "0xGenesisHashInventadoOPrecalculado",
        )
    }

    //analizar

    fun addTransaction(transaction: Transaction): Boolean {
        if (validator.validate(transaction)) {
            pendingTransactions.add(transaction)
            return true
        }
        println("Invalid transaction: ${validator.getErrors()}")
        return false
    }

    fun getLatestBlock(): Block {
        return chain.last()
    }

    fun minePendingTransactions() {
        val newBlock = Block(
            index = getLatestBlock().index + 1,
            timestamp = System.currentTimeMillis(),
            transactions = pendingTransactions.toList(),
            previousHash = getLatestBlock().hash
        )

        newBlock.mineBlock(difficulty)

        chain.add(newBlock)

        pendingTransactions.clear()

        println("Blocked Mined and added")
    }

    fun isChainValid(chainToValidate: List<Block>): Boolean {
        for (i in 1 until chainToValidate.size) {
            val currentBlock = chainToValidate[i]
            val previousBlock = chainToValidate[i - 1]

            if (currentBlock.hash != currentBlock.calculateHash()) {
                return false
            }

            if (currentBlock.previousHash != previousBlock.hash) {
                return false
            }
        }
        return true
    }

    fun replaceChain(newChain: MutableList<Block>) {
        if (newChain.size > chain.size && isChainValid(newChain)) {
            println("Reemplazando la cadena local por una más larga de la red.")
            chain.clear()
            chain.addAll(newChain)
        } else {
            println("La cadena recibida es inválida o más corta. Se rechaza.")
        }
    }
}
