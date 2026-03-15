package entities.transaction.rules

data class ValidationResult(
    val isValid: Boolean,
    val errorList: List<String>,
) {

    fun getErrors(): String =
        if (errorList.isNotEmpty()) {
            errorList.joinToString(separator = " | ")
        } else {
            "No errors found"
        }
}
