package api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "node.identity")
class NodeIdentityProperties {
    lateinit var privateKey: String
    lateinit var publicKey: String
    lateinit var publicUrl: String
}
