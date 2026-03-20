package api

import api.properties.BlockchainProperties
import api.configuration.NetworkProperties
import api.configuration.NodeIdentityProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    BlockchainProperties::class,
    NodeIdentityProperties::class,
    NetworkProperties::class,
)
class KotlinBlockchainApplication

fun main(args: Array<String>) {
    runApplication<KotlinBlockchainApplication>(*args)
}
