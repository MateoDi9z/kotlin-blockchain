package entities

import api.dtos.Transaction
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
    // 1. SEGURIDAD: Listas privadas mutables, expuestas como públicas de solo lectura
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

    fun addTransaction(transaction: Transaction): Boolean {
        // 2. CORRECCIÓN: Uso correcto del ValidationResult (Thread-Safe)
        val result = validator.validate(transaction)

        if (result.isValid) {
            _pendingTransactions.add(transaction)
            return true
        }

        println("Invalid transaction: ${result.getErrors()}")
        return false
    }

    fun getLatestBlock(): Block = _chain.last()

    fun minePendingTransactions() {
        // Creamos el bloque contenedor sin minar
        val unminedBlock =
            Block(
                index = getLatestBlock().index + 1,
                timestamp = System.currentTimeMillis(),
                transactions = _pendingTransactions.toList(),
                previousHash = getLatestBlock().hash,
            )

        // 3. CORRECCIÓN: Atrapamos el bloque resultante ya minado (Inmutabilidad)
        val minedBlock = unminedBlock.mineBlock(difficulty)

        _chain.add(minedBlock)
        _pendingTransactions.clear()

        println("Block Mined and added: ${minedBlock.hash}")
    }

    fun isChainValid(chainToValidate: List<Block>): Boolean {
        for (i in 1 until chainToValidate.size) {
            val currentBlock = chainToValidate[i]
            val previousBlock = chainToValidate[i - 1]

            // 4. CORRECCIÓN: Usamos isValid() que verifica el hash Y la dificultad (PoW)
            if (!currentBlock.isValid(difficulty)) {
                return false
            }

            // Verificamos que la cadena no esté rota
            if (currentBlock.previousHash != previousBlock.hash) {
                return false
            }
        }
        return true
    }

    // Recibe List<Block> genérico para aceptar listas inmutables
    fun replaceChain(newChain: List<Block>) {
        if (newChain.size > _chain.size && isChainValid(newChain)) {
            println("Reemplazando la cadena local por una más larga de la red.")
            _chain.clear()
            _chain.addAll(newChain)
        } else {
            println("La cadena recibida es inválida o más corta. Se rechaza.")
        }
    }
}
