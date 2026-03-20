package api.dtos

data class PeerRegistrationRequest(
    val peerUrl: String,
    val publicKey: String,
    val signature: String,
)
