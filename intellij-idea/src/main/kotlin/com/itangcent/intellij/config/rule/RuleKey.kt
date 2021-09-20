package com.itangcent.intellij.config.rule

import kotlin.reflect.KClass

/**
 * @param T the result type of rule compute
 */
interface RuleKey<T : Any> {
    fun name(): String

    fun alias(): Array<String>?

    @Deprecated(message = "redundant")
    fun ruleType(): KClass<Rule<T>>

    fun mode(): RuleMode<*>

    fun defaultVal(): T?
}

fun RuleKey<*>.nameAndAlias(): Array<String> {
    return alias()?.let { it + name() } ?: arrayOf(name())
}