package api.config

import api.properties.BlockchainProperties
import entities.blockchain.Blockchain
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BlockchainAppConfig(
    private val blockchainProperties: BlockchainProperties,
) {

    @Bean
    fun blockchain(): Blockchain = Blockchain(difficulty = blockchainProperties.difficulty)
}
