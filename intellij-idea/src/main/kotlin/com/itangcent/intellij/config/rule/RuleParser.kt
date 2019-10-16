package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement

@ImplementedBy(SimpleRuleParser::class)
interface RuleParser {
    fun parseStringRule(rule: String): List<StringRule>

    fun parseStringRule(rule: String, delimiters: String): List<StringRule>

    fun parseBooleanRule(rule: String): List<BooleanRule>

    fun parseBooleanRule(rule: String, delimiters: String, defaultValue: Boolean): List<BooleanRule>

    fun contextOf(target: Any, context: PsiElement?): RuleContext
}