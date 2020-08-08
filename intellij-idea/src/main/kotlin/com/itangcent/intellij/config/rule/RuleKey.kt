package com.itangcent.intellij.config.rule

import kotlin.reflect.KClass

interface RuleKey<T> {
    fun name(): String

    fun alias(): Array<String>?

    fun ruleType(): KClass<Rule<T>>

    fun mode(): RuleMode

    fun defaultVal(): T?
}

fun RuleKey<*>.nameAndAlias(): Array<String> {
    return alias()?.let { it + name() } ?: arrayOf(name())
}