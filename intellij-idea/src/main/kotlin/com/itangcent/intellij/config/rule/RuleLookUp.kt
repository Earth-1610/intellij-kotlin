package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import kotlin.reflect.KClass

@ImplementedBy(DefaultRuleLookUp::class)
interface RuleLookUp {
    fun <T : Rule<*>> lookUp(key: String, ruleType: KClass<T>): List<T>
}

fun <T : Rule<*>> RuleLookUp.lookUp(keys: Array<String>, ruleType: KClass<T>): List<T> {
    return keys.flatMap { this.lookUp(it, ruleType) }
}