package com.itangcent.intellij.config.rule

import com.google.inject.ImplementedBy
import kotlin.reflect.KClass

@ImplementedBy(DefaultRuleLookUp::class)
interface RuleLookUp {
    fun <T : Any> lookUp(key: String, ruleType: KClass<T>): List<Rule<T>>
}

fun <T : Any> RuleLookUp.doLookUp(keys: Array<String>, ruleType: KClass<T>): List<Rule<T>> {
    return keys.flatMap { this.lookUp(it, ruleType) }
}