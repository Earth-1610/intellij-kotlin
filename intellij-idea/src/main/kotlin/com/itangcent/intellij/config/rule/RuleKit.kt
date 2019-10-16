package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiElement
import com.itangcent.common.utils.reduceSafely
import com.itangcent.intellij.context.ActionContext


fun List<StringRule>?.compute(target: Any, context: PsiElement?, mode: StringRuleMode = StringRuleMode.MERGE): String? {
    if (this.isNullOrEmpty()) {
        return null
    }
    val psiElementContext = ActionContext.getContext()!!.instance(RuleParser::class).contextOf(target, context)
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

fun List<BooleanRule>?.compute(
    target: Any,
    context: PsiElement?,
    mode: BooleanRuleMode = BooleanRuleMode.ANY
): Boolean? {
    if (this.isNullOrEmpty()) {
        return null
    }
    val psiElementContext = ActionContext.getContext()!!.instance(RuleParser::class).contextOf(target, context)
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
