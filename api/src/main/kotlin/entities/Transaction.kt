package api.entities

class Transaction(
    val from: String,
    val to: String,
    val amount: Float,
    val currency: String,
    val signature: String
) {
    fun isValid(): Boolean {

    }
}