package entities.blockchain.results

sealed interface TransactionAdditionResult {
    object Success : TransactionAdditionResult

    data class Rejected(
        val errors: String,
    ) : TransactionAdditionResult
}
