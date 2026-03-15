package entities.blockchain.results

sealed class TransactionAdditionResult {
    object Success : TransactionAdditionResult()

    data class Rejected(
        val errors: String,
    ) : TransactionAdditionResult()
}
