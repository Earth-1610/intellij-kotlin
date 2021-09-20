package com.itangcent.intellij.config.rule

import com.itangcent.common.utils.asStream
import com.itangcent.common.utils.notNullOrEmpty
import com.itangcent.common.utils.reduceSafely
import kotlin.reflect.KClass

interface RuleMode<T : Any> {
    fun compute(rules: RuleChain<T>): Any?

    fun targetType(): KClass<T>
}

enum class StringRuleMode : RuleMode<String> {
    SINGLE {
        override fun compute(rules: RuleChain<String>): Any? {
            return rules
                .asStream()
                .map { it.compute() }
                .filter { it.notNullOrEmpty() }
                .findFirst()
                .orElse(null)
        }
    },
    MERGE {
        override fun compute(rules: RuleChain<String>): Any? {
            return rules
                .asStream()
                .map { it.compute() }
                .filter { it.notNullOrEmpty() }
                .reduceSafely { s1, s2 -> "$s1\n$s2" }
        }
    },
    MERGE_DISTINCT {
        override fun compute(rules: RuleChain<String>): Any? {
            return rules
                .asStream()
                .map { it.compute() }
                .filter { it.notNullOrEmpty() }
                .distinct()
                .reduceSafely { s1, s2 -> "$s1\n$s2" }
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
                .asStream()
                .map { it.compute() }
                .anyMatch { it == true }
        }
    },
    ALL {
        override fun compute(rules: RuleChain<Boolean>): Any {
            return rules
                .asStream()
                .map { it.compute() }
                .allMatch { it == true }
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
                    it.compute()
                } catch (ignore: Exception) {
                }
            }
            return null
        }
    },
    THROW_IN_ERROR {
        override fun compute(rules: RuleChain<Unit>): Any? {
            rules.forEach {
                it.compute()
            }
            return null
        }
    };

    override fun targetType(): KClass<Unit> {
        return Unit::class
    }
}