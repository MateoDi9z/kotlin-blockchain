package api.entities.blockchain.results

sealed interface ChainSyncResult {
    object Success : ChainSyncResult

    data class Rejected(
        val reason: String,
    ) : ChainSyncResult
}
