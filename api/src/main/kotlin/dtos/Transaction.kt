package api.dtos


data class Transaction(
    val from: String,
    val to: String,
    val amount: Float,
    val signature: String,
)
