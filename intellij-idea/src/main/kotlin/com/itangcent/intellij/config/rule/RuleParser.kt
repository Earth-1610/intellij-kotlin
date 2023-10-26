package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

@ImplementedBy(SimpleRuleParser::class)
interface RuleParser {

    fun parseRule(rule: String, targetType: KClass<*>): Rule<Any>?

    fun contextOf(target: Any, context: PsiElement?): RuleContext
}

fun RuleParser.parseAnyRule(rule: String): AnyRule? = parseRule(rule, Any::class)

@Suppress("UNCHECKED_CAST")
fun RuleParser.parseStringRule(rule: String) = parseRule(rule, String::class) as StringRule?

@Suppress("UNCHECKED_CAST")
fun RuleParser.parseBooleanRule(rule: String) = parseRule(rule, Boolean::class) as BooleanRule?

@Suppress("UNCHECKED_CAST")
fun RuleParser.parseEventRule(rule: String) = parseRule(rule, Unit::class) as EventRule?

abstract class AbstractRuleParser : RuleParser {
    companion object {
        private val NON: Rule<*> = {}
    }

    private val ruleParseCache: HashMap<String, Rule<Any>> = HashMap()

    override fun parseRule(rule: String, targetType: KClass<*>): Rule<Any>? {
        if (rule.isBlank()) return null

        return ruleParseCache.computeIfAbsent("$targetType:$rule") {
            val trimRule = rule.trim()
            when (targetType) {
                String::class -> {
                    parseStringRule(trimRule)
                }

                Boolean::class -> {
                    parseBooleanRule(trimRule)
                }

                Unit::class -> {
                    parseEventRule(trimRule)
                }

                else -> {
                    parseAnyRule(trimRule)
                }
            } ?: NON
        }.takeIf { it !== NON }
    }

    abstract fun parseAnyRule(rule: String): AnyRule?

    abstract fun parseStringRule(rule: String): StringRule?

    abstract fun parseBooleanRule(rule: String): BooleanRule?

    abstract fun parseEventRule(rule: String): EventRule?

}