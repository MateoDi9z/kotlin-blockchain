package entities.results

sealed interface OperationResult<out T> {

    data class Success<out T>(
        val data: T,
    ) : OperationResult<T>

    data class Failure(
        val errors: List<String>,
    ) : OperationResult<Nothing> {

        fun getErrors(): String =
            if (errors.isNotEmpty()) {
                errors.joinToString(separator = " | ")
            } else {
                "No errors found"
            }
    }
}
