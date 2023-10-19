package com.itangcent.intellij.config.rule

import com.itangcent.common.utils.notNullOrEmpty
import kotlin.reflect.KClass

interface RuleMode<T : Any> {
    fun compute(rules: RuleChain<T>): Any?

    fun targetType(): KClass<T>
}

enum class StringRuleMode : RuleMode<String> {
    SINGLE {
        override fun compute(rules: RuleChain<String>): Any? {
            return rules
                .map { it() }
                .filter { it.notNullOrEmpty() }
                .firstOrNull()
        }
    },
    MERGE {
        override fun compute(rules: RuleChain<String>): Any {
            return rules
                .map { it() }
                .filter { it.notNullOrEmpty() }
                .joinToString(separator = "\n")
        }
    },
    MERGE_DISTINCT {
        override fun compute(rules: RuleChain<String>): Any {
            return rules
                .map { it() }
                .filter { it.notNullOrEmpty() }
                .distinct()
                .joinToString(separator = "\n")
        }
    };

    override fun targetType(): KClass<String> {
        return String::class
    }
}

enum class BooleanRuleMode : RuleMode<Boolean> {
    ANY {
        override fun compute(rules: RuleChain<Boolean>): Any {
            return rules
                .map { it() }
                .any { it == true }
        }
    },
    ALL {
        override fun compute(rules: RuleChain<Boolean>): Any {
            return rules
                .map { it() }
                .all { it == true }
        }
    };

    override fun targetType(): KClass<Boolean> {
        return Boolean::class
    }
}

enum class EventRuleMode : RuleMode<Unit> {
    IGNORE_ERROR {
        override fun compute(rules: RuleChain<Unit>): Any? {
            rules.forEach {
                try {
                    it()
                } catch (ignore: Exception) {
                }
            }
            return null
        }
    },
    THROW_IN_ERROR {
        override fun compute(rules: RuleChain<Unit>): Any? {
            rules.forEach {
                it()
            }
            return null
        }
    };

    override fun targetType(): KClass<Unit> {
        return Unit::class
    }
}