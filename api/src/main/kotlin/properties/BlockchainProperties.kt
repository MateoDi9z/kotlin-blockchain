package api.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "blockchain")
class BlockchainProperties {
    var difficulty: Int = 3
    var bootstrapNodes: List<String> = emptyList()
}
