package api.entities.results

data class PeerRegistrationResult(
    val isSuccess: Boolean,
    val knownPeers: Set<String> = emptySet(),
    val errorMessage: String? = null,
)
