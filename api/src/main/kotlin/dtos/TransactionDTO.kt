package api.dtos


data class TransactionDTO(
    val from: String,
    val to: String,
    val amount: Float,
    val signature: String,
)
