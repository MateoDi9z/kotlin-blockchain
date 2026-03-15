package block

import api.dtos.Transaction
import api.entities.Block
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockTest {

    @Test
    fun initialization_setsAllProperties() {
        val tx = Transaction(from = "alice", to = "bob", amount = 10.0f, signature = "sig")
        val transactions = listOf(tx)
        val block =
            Block(
                index = 5,
                timestamp = 1_630_000_000L,
                transactions = transactions,
                previousHash = "prevHashValue",
                hash = "currentHashValue",
                nonce = 123L,
            )

        assertEquals(5, block.index)
        assertEquals(1_630_000_000L, block.timestamp)
        assertEquals(transactions, block.transactions)
        assertEquals("prevHashValue", block.previousHash)
        assertEquals("currentHashValue", block.hash)
        assertEquals(123L, block.nonce)
    }

    @Test
    fun defaultNonce_isZeroWhenNotProvided() {
        val block =
            Block(
                index = 0,
                timestamp = 0L,
                transactions = emptyList(),
                previousHash = "",
                hash = "",
            )

        assertEquals(0L, block.nonce)
    }

    @Test
    fun emptyTransactionsList_isAllowed() {
        val block =
            Block(
                index = 2,
                timestamp = 1L,
                transactions = emptyList(),
                previousHash = "prev",
                hash = "h",
            )

        assertTrue(block.transactions.isEmpty())
    }
}
