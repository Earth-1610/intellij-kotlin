package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement


@ImplementedBy(DefaultRuleComputer::class)
interface RuleComputer {

    fun <T> computer(ruleKey: RuleKey<T>, element: PsiElement): T? {
        return computer(ruleKey, element, element)
    }

    fun <T> computer(ruleKey: RuleKey<T>, target: Any, context: PsiElement?): T?
}