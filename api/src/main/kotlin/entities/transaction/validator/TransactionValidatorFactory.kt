package entities.transaction.validator

import api.entities.transaction.rules.PositiveAmountRule
import api.entities.transaction.rules.SignatureNotEmptyRule
import api.entities.transaction.rules.TransactionRule
import api.entities.transaction.rules.ValidCryptographicSignatureRule

object TransactionValidatorFactory {

    fun createDefault(): TransactionValidator {
        val rules =
            listOf(
                PositiveAmountRule(),
                SignatureNotEmptyRule(),
                ValidCryptographicSignatureRule(),
            )
        return TransactionValidator(rules)
    }

    fun createWithCustomRules(rules: List<TransactionRule>): TransactionValidator =
        TransactionValidator(rules)
}
