package com.itangcent.intellij.config.rule

import kotlin.reflect.KClass

class SimpleRuleKey<T> : RuleKey<T> {

    private val name: String
    private val alias: Array<String>?
    private val ruleType: KClass<Rule<T>>
    private val mode: RuleMode
    private val defaultVal: T?

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, ruleType: KClass<*>, mode: RuleMode) {
        this.name = name
        this.ruleType = ruleType as KClass<Rule<T>>
        this.mode = mode
        this.defaultVal = null
        this.alias = null
    }

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, alias: Array<String>?, ruleType: KClass<*>, mode: RuleMode) {
        this.name = name
        this.alias = alias
        this.ruleType = ruleType as KClass<Rule<T>>
        this.mode = mode
        this.defaultVal = null
    }

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, ruleType: KClass<*>, mode: RuleMode, defaultVal: T?) {
        this.name = name
        this.alias = null
        this.ruleType = ruleType as KClass<Rule<T>>
        this.mode = mode
        this.defaultVal = defaultVal
    }

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, alias: Array<String>, ruleType: KClass<*>, mode: RuleMode, defaultVal: T?) {
        this.name = name
        this.alias = alias
        this.ruleType = ruleType as KClass<Rule<T>>
        this.mode = mode
        this.defaultVal = defaultVal
    }


    override fun name(): String {
        return name
    }

    override fun ruleType(): KClass<Rule<T>> {
        return ruleType
    }

    override fun mode(): RuleMode {
        return mode
    }

    override fun defaultVal(): T? {
        return defaultVal
    }

    override fun alias(): Array<String>? {
        return this.alias
    }
}