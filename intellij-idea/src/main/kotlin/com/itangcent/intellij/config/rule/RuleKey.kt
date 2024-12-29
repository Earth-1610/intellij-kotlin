package com.itangcent.intellij.config.rule

/**
 * @param T the result type of rule compute
 */
interface RuleKey<T : Any> {
    fun name(): String

    fun alias(): Array<String>?

    fun mode(): RuleMode<*>

    fun defaultVal(): T?
}

fun RuleKey<*>.nameAndAlias(): Array<String> {
    return alias()?.let { it + name() } ?: arrayOf(name())
}