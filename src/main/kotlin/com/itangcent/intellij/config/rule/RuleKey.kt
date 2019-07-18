package com.itangcent.intellij.config.rule

import kotlin.reflect.KClass

interface RuleKey<T> {
    fun name(): String

    fun ruleType(): KClass<Rule<T>>

    fun mode(): RuleMode

    fun defaultVal(): T?
}