package entities.results

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

    fun isSuccess(): Boolean = isValid

    fun isFailure(): Boolean = !isValid
}
