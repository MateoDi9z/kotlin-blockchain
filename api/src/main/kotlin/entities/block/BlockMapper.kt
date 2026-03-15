package api.entities.block

import dtos.BlockDTO
import entities.block.Block

object BlockMapper {
    fun toEntity(dto: BlockDTO): Block =
        Block(
            index = dto.index,
            timestamp = dto.timestamp,
            transactions = dto.transactions,
            previousHash = dto.previousHash,
            hash = dto.hash,
            nonce = dto.nonce,
        )
}
