package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiElement


interface RuleComputeListener {
    fun computer(
        ruleKey: RuleKey<*>,
        target: Any,
        context: PsiElement?,
        contextHandle: (RuleContext) -> Unit,
        methodHandle: (RuleKey<*>, Any, PsiElement?, (RuleContext) -> Unit) -> Any?
    ): Any?

}