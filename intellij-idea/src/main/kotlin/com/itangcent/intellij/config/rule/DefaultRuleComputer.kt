package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement

@Singleton
class DefaultRuleComputer : RuleComputer {

    @Inject
    protected val ruleLookUp: RuleLookUp? = null

    @Suppress("UNCHECKED_CAST")
    override fun <T> computer(ruleKey: RuleKey<T>, target: Any, context: PsiElement?): T? {
        val rules = ruleLookUp!!.lookUp(ruleKey.name(), ruleKey.ruleType())

        if (rules.isNullOrEmpty()) return ruleKey.defaultVal()

        return (when (ruleKey.mode()) {
            is StringRuleMode -> {
                (rules as (List<StringRule>)).compute(target, context, ruleKey.mode() as StringRuleMode) as T?
            }
            is BooleanRuleMode -> {
                (rules as (List<BooleanRule>)).compute(target, context, ruleKey.mode() as BooleanRuleMode) as T?
            }
            else -> null
        }) ?: ruleKey.defaultVal()
    }
}