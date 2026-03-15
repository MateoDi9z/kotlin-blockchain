package entities.block.rule

object BlockValidatorFactory {

    fun createDefault(): BlockValidator {
        val rulesList =
            listOf(
                ChainLinkRule(),
                ValidHashRule(),
                ProofOfWorkRule(),
            )

        val compositeRule = CompositeBlockRule(rulesList)

        return BlockValidator(compositeRule)
    }

    fun createWithCustomRules(rules: List<BlockRule>): BlockValidator =
        BlockValidator(CompositeBlockRule(rules))
}
