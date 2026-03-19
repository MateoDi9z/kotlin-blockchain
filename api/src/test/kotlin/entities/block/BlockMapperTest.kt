package entities.block

import dtos.BlockDTO
import api.dtos.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockMapperTest {

    @Test
    fun `toEntity maps all fields`() {
        val tx = Transaction("a", "b", 1L, "sig")
        val dto =
            BlockDTO(
                index = 2,
                timestamp = 123L,
                transactions = listOf(tx),
                previousHash = "0",
                hash = "h",
                nonce = 5L,
            )

        val entity = BlockMapper.toEntity(dto)

        assertEquals(dto.index, entity.index)
        assertEquals(dto.timestamp, entity.timestamp)
        assertEquals(dto.transactions, entity.transactions)
        assertEquals(dto.previousHash, entity.previousHash)
        assertEquals(dto.hash, entity.hash)
        assertEquals(dto.nonce, entity.nonce)
    }
}
