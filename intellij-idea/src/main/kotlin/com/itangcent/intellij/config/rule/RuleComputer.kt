package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.google.inject.Singleton
import com.intellij.psi.PsiElement


@ImplementedBy(DefaultRuleComputer::class)
interface RuleComputer {

    fun <T> computer(ruleKey: RuleKey<T>, element: PsiElement): T?
}