package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiElement
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.util.reduceSafely

fun List<StringRule>?.compute(element: PsiElement, mode: StringRuleMode = StringRuleMode.MERGE): String? {
    if (this.isNullOrEmpty()) {
        return null
    }
    val context = ActionContext.getContext()!!.instance(RuleParser::class).contextOf(element)
    return when (mode) {
        StringRuleMode.SINGLE -> this.map { it.compute(context) }
            .firstOrNull { !it.isNullOrEmpty() }
        StringRuleMode.MERGE -> this.map { it.compute(context) }
            .filter { !it.isNullOrEmpty() }
            .reduceSafely { s1, s2 -> "$s1\n$s2" }
        StringRuleMode.MERGE_DISTINCT -> this.map { it.compute(context) }
            .filter { !it.isNullOrEmpty() }
            .distinct()
            .reduceSafely { s1, s2 -> "$s1\n$s2" }
    }
}


fun List<BooleanRule>?.compute(element: PsiElement, mode: BooleanRuleMode = BooleanRuleMode.ANY): Boolean? {
    if (this.isNullOrEmpty()) {
        return null
    }
    val context = ActionContext.getContext()!!.instance(RuleParser::class).contextOf(element)
    return when (mode) {
        BooleanRuleMode.ANY -> this.map { it.compute(context) }
            .firstOrNull { it == true }
        BooleanRuleMode.ALL -> this.map { it.compute(context) }
            .all { it == true }
    }
}
