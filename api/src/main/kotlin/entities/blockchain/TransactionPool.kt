package entities.blockchain

import api.dtos.Transaction
import entities.results.OperationResult
import entities.transaction.validator.TransactionValidator
import entities.transaction.validator.TransactionValidatorFactory

class TransactionPool(
    private val txValidator: TransactionValidator = TransactionValidatorFactory.createDefault(),
) {
    private val _pendingTransactions = mutableListOf<Transaction>()

    val pendingTransactions: List<Transaction> get() = _pendingTransactions.toList()

    fun addTransaction(transaction: Transaction): OperationResult<Unit> {
        val validation = txValidator.validate(transaction)

        return if (validation.isValid) {
            _pendingTransactions.add(transaction)
            OperationResult.Success(Unit)
        } else {
            OperationResult.Failure(validation.errorList)
        }
    }

    fun extractTransactionsForMining(): List<Transaction> {
        val transactionsToMine = _pendingTransactions.toList()
        _pendingTransactions.clear()
        return transactionsToMine
    }

    fun clear() {
        _pendingTransactions.clear()
    }
}
