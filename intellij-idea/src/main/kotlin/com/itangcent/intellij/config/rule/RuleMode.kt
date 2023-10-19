package com.itangcent.intellij.config.rule

import com.itangcent.common.utils.asInt
import com.itangcent.common.utils.notNullOrEmpty
import kotlin.reflect.KClass

interface RuleMode<T : Any> {
    fun compute(rules: RuleChain<T>): Any?

    fun targetType(): KClass<T>
}

open class AnyRuleMode<T : Any>(
    val computer: (Sequence<Any>) -> T?,
    private val targetType: KClass<T>
) : RuleMode<T> {
    override fun compute(rules: RuleChain<T>): T? {
        return computer(rules.mapNotNull { it() })
    }

    override fun targetType(): KClass<T> {
        return targetType
    }
}

object IntRuleMode : AnyRuleMode<Int>(
    computer = { sequence -> sequence.map { it.asInt() }.firstOrNull() },
    targetType = Int::class
)

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
                .mapNotNull { it() }
                .any { it }
        }
    },
    ALL {
        override fun compute(rules: RuleChain<Boolean>): Any {
            return rules
                .mapNotNull { it() }
                .all { it }
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