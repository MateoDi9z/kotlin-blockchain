package api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "blockchain.network")
class NetworkProperties {
    var knownPeers: List<String> = emptyList()
}
