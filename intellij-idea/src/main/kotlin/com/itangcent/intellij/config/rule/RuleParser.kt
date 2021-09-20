package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import kotlin.reflect.KClass

@ImplementedBy(SimpleRuleParser::class)
interface RuleParser {

    fun parseRule(rule: String, targetType: KClass<*>): Rule<*>? {
        return when (targetType) {
            String::class -> {
                parseStringRule(rule)
            }
            Boolean::class -> {
                parseBooleanRule(rule)
            }
            Unit::class -> {
                parseEventRule(rule)
            }
            else -> {
                ActionContext.getContext()?.instance(Logger::class)
                    ?.warn("rule of ${targetType.qualifiedName}  not be supported for ${this::class.qualifiedName}.")
                null
            }
        }
    }

    fun parseStringRule(rule: String): StringRule?

    fun parseBooleanRule(rule: String): BooleanRule?

    fun parseEventRule(rule: String): EventRule?

    fun contextOf(target: Any, context: PsiElement?): RuleContext
}