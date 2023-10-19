package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

@Singleton
class DefaultRuleComputer : RuleComputer {

    @Inject
    protected lateinit var ruleLookUp: RuleLookUp

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
        val rules: List<Rule<Any>> = ruleLookUp.lookUp(ruleKey)

        if (rules.isEmpty()) return ruleKey.defaultVal()

        val ruleContext: RuleContext = ruleParser!!.contextOf(target, context)
        contextHandle(ruleContext)

        return (ruleKey.mode() as RuleMode<Any>).compute(rules.asChain(ruleContext)) as? T
            ?: ruleKey.defaultVal()
    }

    private fun <T> List<Rule<T>>.asChain(ruleContext: RuleContext): RuleChain<T> =
        asSequence().map { { it(ruleContext) } }
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> RuleLookUp.lookUp(ruleKey: RuleKey<T>) = lookUp(
    ruleKey.nameAndAlias(),
    ruleKey.mode().targetType() as KClass<Any>
)
