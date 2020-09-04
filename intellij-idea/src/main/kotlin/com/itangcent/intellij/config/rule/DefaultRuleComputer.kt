package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import com.itangcent.common.utils.reduceSafely

@Singleton
class DefaultRuleComputer : RuleComputer {

    @Inject
    protected val ruleLookUp: RuleLookUp? = null

    @Inject
    protected val ruleParser: RuleParser? = null

    @Suppress("UNCHECKED_CAST")
    override fun <T> computer(
        ruleKey: RuleKey<T>,
        target: Any,
        context: PsiElement?,
        contextHandle: (RuleContext) -> Unit
    ): T? {
        val rules = ruleLookUp!!.lookUp(ruleKey.nameAndAlias(), ruleKey.ruleType())

        if (rules.isNullOrEmpty()) return ruleKey.defaultVal()

        return (when (ruleKey.mode()) {
            is StringRuleMode -> {
                (rules as (List<StringRule>)).compute(
                    target,
                    context,
                    ruleKey.mode() as StringRuleMode,
                    contextHandle
                ) as T?
            }
            is BooleanRuleMode -> {
                (rules as (List<BooleanRule>)).compute(
                    target,
                    context,
                    ruleKey.mode() as BooleanRuleMode,
                    contextHandle
                ) as T?
            }
            is EventRuleMode -> {
                (rules as (List<EventRule>)).compute(
                    target,
                    context,
                    ruleKey.mode() as EventRuleMode,
                    contextHandle
                ) as T?
            }
            else -> null
        }) ?: ruleKey.defaultVal()
    }


    private fun List<StringRule>?.compute(
        target: Any,
        context: PsiElement?,
        mode: StringRuleMode = StringRuleMode.MERGE,
        contextHandle: (RuleContext) -> Unit
    ): String? {
        if (this.isNullOrEmpty()) {
            return null
        }
        val psiElementContext = ruleParser!!.contextOf(target, context)
        contextHandle(psiElementContext)
        return when (mode) {
            StringRuleMode.SINGLE -> this
                .stream()
                .map { it.compute(psiElementContext) }
                .filter { !it.isNullOrEmpty() }
                .findFirst()
                .orElse(null)
            StringRuleMode.MERGE -> this
                .map { it.compute(psiElementContext) }
                .filter { !it.isNullOrEmpty() }
                .reduceSafely { s1, s2 -> "$s1\n$s2" }
            StringRuleMode.MERGE_DISTINCT -> this.map { it.compute(psiElementContext) }
                .filter { !it.isNullOrEmpty() }
                .distinct()
                .reduceSafely { s1, s2 -> "$s1\n$s2" }
        }
    }

    private fun List<BooleanRule>?.compute(
        target: Any,
        context: PsiElement?,
        mode: BooleanRuleMode = BooleanRuleMode.ANY,
        contextHandle: (RuleContext) -> Unit
    ): Boolean? {
        if (this.isNullOrEmpty()) {
            return null
        }
        val psiElementContext = ruleParser!!.contextOf(target, context)
        contextHandle(psiElementContext)
        return when (mode) {
            BooleanRuleMode.ANY -> this
                .stream()
                .map { it.compute(psiElementContext) }
                .anyMatch { it == true }
            BooleanRuleMode.ALL -> this
                .stream()
                .map { it.compute(psiElementContext) }
                .allMatch { it == true }
        }
    }

    private fun List<EventRule>?.compute(
        target: Any,
        context: PsiElement?,
        mode: EventRuleMode = EventRuleMode.IGNORE_ERROR,
        contextHandle: (RuleContext) -> Unit
    ) {
        if (this.isNullOrEmpty()) {
            return
        }
        val psiElementContext = ruleParser!!.contextOf(target, context)
        contextHandle(psiElementContext)
        when (mode) {
            EventRuleMode.IGNORE_ERROR -> this.forEach {
                try {
                    it.compute(psiElementContext)
                } catch (ignore: Exception) {
                }
            }
            EventRuleMode.THROW_IN_ERROR -> this.forEach {
                it.compute(psiElementContext)
            }
        }
    }
}