package entities.block

import api.entities.transaction.rules.PositiveAmountRule
import entities.block.rule.BlockValidator
import entities.transaction.validator.TransactionValidator
import testutils.TestBuilders
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockValidatorTest {

    private val txValidator = TransactionValidator(listOf(PositiveAmountRule()))
    private val difficulty = 2

    @Test
    fun validateReceivedBlock_returnsTrue_whenEverythingIsValid() {
        val prevBlock = TestBuilders.makeMinedBlock(difficulty, index = 1, previousHash = "0")

        val validTx = TestBuilders.makeTransaction(amount = 10L)
        val newBlock =
            TestBuilders.makeMinedBlock(
                difficulty = difficulty,
                index = 2,
                previousHash = prevBlock.hash,
                transactions = listOf(validTx),
            )
        val newBlockDto = TestBuilders.toDto(newBlock)

        val result =
            BlockValidator.validateReceivedBlock(
                newBlockDto,
                difficulty,
                prevBlock,
                txValidator,
            )

        assertTrue(result, "El bloque debería ser aceptado porque todo es correcto")
    }

    @Test
    fun validateReceivedBlock_returnsFalse_whenTransactionsAreInvalid() {
        val prevBlock = TestBuilders.makeMinedBlock(difficulty, index = 1, previousHash = "0")

        val invalidTx = TestBuilders.makeTransaction(amount = -5L)

        val newBlock =
            TestBuilders.makeMinedBlock(
                difficulty = difficulty,
                index = 2,
                previousHash = prevBlock.hash,
                transactions = listOf(invalidTx),
            )
        val newBlockDto = TestBuilders.toDto(newBlock)

        val result =
            BlockValidator.validateReceivedBlock(
                newBlockDto,
                difficulty,
                prevBlock,
                txValidator,
            )

        assertFalse(result, "El bloque debería ser rechazado por contener transacciones inválidas")
    }

    @Test
    fun validateReceivedBlock_returnsFalse_whenChainLinkIsBroken() {
        val prevBlock = TestBuilders.makeMinedBlock(difficulty, index = 1, previousHash = "0")
        val validTx = TestBuilders.makeTransaction(amount = 10L)

        val newBlock =
            TestBuilders.makeMinedBlock(
                difficulty = difficulty,
                index = 2,
                previousHash = "hash_falso",
                transactions = listOf(validTx),
            )
        val newBlockDto = TestBuilders.toDto(newBlock)

        val result =
            BlockValidator.validateReceivedBlock(
                newBlockDto,
                difficulty,
                prevBlock,
                txValidator,
            )

        assertFalse(result, "El bloque debería ser rechazado por no apuntar al bloque anterior")
    }

    @Test
    fun validateReceivedBlock_returnsFalse_whenIntegrityIsTampered() {
        val prevBlock = TestBuilders.makeMinedBlock(difficulty, index = 1, previousHash = "0")
        val validTx = TestBuilders.makeTransaction(amount = 10L)

        val originalBlock =
            TestBuilders.makeMinedBlock(
                difficulty = difficulty,
                index = 2,
                previousHash = prevBlock.hash,
                transactions = listOf(validTx),
            )

        val tamperedBlock =
            Block(
                index = originalBlock.index,
                timestamp = originalBlock.timestamp,
                transactions = originalBlock.transactions,
                previousHash = originalBlock.previousHash,
                hash = "hash_manipulado",
                nonce = originalBlock.nonce,
            )
        val tamperedDto = TestBuilders.toDto(tamperedBlock)

        val result =
            BlockValidator.validateReceivedBlock(
                tamperedDto,
                difficulty,
                prevBlock,
                txValidator,
            )

        assertFalse(result, "El bloque debería ser rechazado porque su hash fue alterado")
    }
}
