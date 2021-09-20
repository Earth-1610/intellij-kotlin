package com.itangcent.intellij.config.rule

import kotlin.reflect.KClass

class SimpleRuleKey<T : Any> : RuleKey<T> {

    private val name: String
    private val alias: Array<String>?
    private val mode: RuleMode<*>
    private val defaultVal: T?

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, mode: RuleMode<*>) {
        this.name = name
        this.mode = mode
        this.defaultVal = null
        this.alias = null
    }

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, alias: Array<String>?, mode: RuleMode<*>) {
        this.name = name
        this.alias = alias
        this.mode = mode
        this.defaultVal = null
    }

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, mode: RuleMode<*>, defaultVal: T?) {
        this.name = name
        this.alias = null
        this.mode = mode
        this.defaultVal = defaultVal
    }

    @Suppress("UNCHECKED_CAST")
    constructor(name: String, alias: Array<String>, mode: RuleMode<*>, defaultVal: T?) {
        this.name = name
        this.alias = alias
        this.mode = mode
        this.defaultVal = defaultVal
    }


    @Deprecated(message = "ruleType is unnecessary")
    @Suppress("UNCHECKED_CAST")
    constructor(name: String, ruleType: KClass<*>, mode: RuleMode<*>) {
        this.name = name
        this.mode = mode
        this.defaultVal = null
        this.alias = null
    }

    @Deprecated(message = "ruleType is unnecessary")
    @Suppress("UNCHECKED_CAST")
    constructor(name: String, alias: Array<String>?, ruleType: KClass<*>, mode: RuleMode<*>) {
        this.name = name
        this.alias = alias
        this.mode = mode
        this.defaultVal = null
    }

    @Deprecated(message = "ruleType is unnecessary")
    @Suppress("UNCHECKED_CAST")
    constructor(name: String, ruleType: KClass<*>, mode: RuleMode<*>, defaultVal: T?) {
        this.name = name
        this.alias = null
        this.mode = mode
        this.defaultVal = defaultVal
    }

    @Deprecated(message = "ruleType is unnecessary")
    @Suppress("UNCHECKED_CAST")
    constructor(name: String, alias: Array<String>, ruleType: KClass<*>, mode: RuleMode<*>, defaultVal: T?) {
        this.name = name
        this.alias = alias
        this.mode = mode
        this.defaultVal = defaultVal
    }


    override fun name(): String {
        return name
    }

    override fun ruleType(): KClass<Rule<T>> {
        throw NotImplementedError("ruleType is not implemented")
    }

    override fun mode(): RuleMode<*> {
        return mode
    }

    override fun defaultVal(): T? {
        return defaultVal
    }

    override fun alias(): Array<String>? {
        return this.alias
    }
}