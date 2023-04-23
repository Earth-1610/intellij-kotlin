package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

@Singleton
class DefaultRuleComputer : RuleComputer {

    @Inject
    protected val ruleLookUp: RuleLookUp? = null

    @Inject
    protected val ruleParser: RuleParser? = null

    @Inject(optional = true)
    protected val ruleComputeListener: RuleComputeListener? = null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> computer(
        ruleKey: RuleKey<T>,
        target: Any,
        context: PsiElement?,
        contextHandle: (RuleContext) -> Unit
    ): T? {
        return if (ruleComputeListener != null) {
            ruleComputeListener.computer(
                ruleKey, target, context, contextHandle
            ) { rk, tg, ct, cth -> doComputer(rk, tg, ct, cth) } as? T
        } else {
            doComputer(ruleKey, target, context, contextHandle)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> doComputer(
        ruleKey: RuleKey<T>,
        target: Any,
        context: PsiElement?,
        contextHandle: (RuleContext) -> Unit
    ): T? {
        val rules: List<Rule<Any>> =
            ruleLookUp!!.doLookUp(
                ruleKey.nameAndAlias(),
                ruleKey.mode().targetType() as KClass<Any>
            )

        if (rules.isNullOrEmpty()) return ruleKey.defaultVal()

        val ruleContext: RuleContext = ruleParser!!.contextOf(target, context)
        contextHandle(ruleContext)

        return (ruleKey.mode() as RuleMode<Any>).compute(RuleChainImpl(rules, ruleContext)) as? T
            ?: ruleKey.defaultVal()
    }


    private class RuleChainImpl<T>(
        private val rules: List<Rule<T>>,
        private val ruleContext: RuleContext,
        private val index: Int = 0
    ) : RuleChain<T> {

        override fun nextChain(): RuleChain<T>? {
            val nextIndex = index + 1
            return if (nextIndex < rules.size)
                RuleChainImpl(rules, ruleContext, nextIndex)
            else null
        }

        override fun compute(): T? {
            return rules[index].compute(ruleContext)
        }
    }
}