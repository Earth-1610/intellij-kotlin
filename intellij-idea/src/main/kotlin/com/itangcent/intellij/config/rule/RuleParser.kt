package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement

@ImplementedBy(SimpleRuleParser::class)
interface RuleParser {

    fun parseStringRule(rule: String): StringRule?

    fun parseBooleanRule(rule: String): BooleanRule?

    fun contextOf(target: Any, context: PsiElement?): RuleContext
}