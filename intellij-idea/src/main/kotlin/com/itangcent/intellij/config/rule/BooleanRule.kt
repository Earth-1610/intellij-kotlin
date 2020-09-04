package com.itangcent.intellij.config.rule

import com.itangcent.intellij.config.rule.BooleanRule.Companion.of

interface BooleanRule : Rule<Boolean> {

    fun inverse(): BooleanRule {
        val origin = this
        return of { context ->
            origin.compute(context)?.let { !it }
        }
    }

    companion object {
        fun of(result: Boolean): BooleanRule {
            return object : BooleanRule {
                override fun compute(context: RuleContext): Boolean? {
                    return result
                }
            }
        }

        fun of(booleanRule: (RuleContext) -> Boolean?): BooleanRule {
            return object : BooleanRule {
                override fun compute(context: RuleContext): Boolean? {
                    return booleanRule(context)
                }
            }
        }

        fun filterWith(filter: BooleanRule, booleanRule: BooleanRule): BooleanRule {
            return of { context ->
                if (filter.compute(context) == true) {
                    return@of booleanRule.compute(context)
                } else {
                    return@of null
                }
            }
        }

        fun filterWith(filter: BooleanRule, eventRule: EventRule): EventRule {
            return EventRule.of { context ->
                if (filter.compute(context) == true) {
                    eventRule.compute(context)
                }
            }
        }
    }
}

fun Collection<BooleanRule>.any(context: RuleContext): Boolean {
    return this.any { it.compute(context) == true }
}

fun Collection<BooleanRule>?.union(): BooleanRule {
    return when {
        this.isNullOrEmpty() -> of {
            false
        }
        this.size == 1 -> this.first()
        else -> of { this.any(it) }
    }
}