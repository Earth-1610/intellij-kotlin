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
        fun of(booleanRule: (RuleContext) -> Boolean?): BooleanRule {
            return object : BooleanRule {
                override fun compute(context: RuleContext): Boolean? {
                    return booleanRule(context)
                }
            }
        }

        fun filterWith(boolRule: BooleanRule, booleanRule: BooleanRule): BooleanRule {
            return of { context ->
                if (boolRule.compute(context) == true) {
                    return@of booleanRule.compute(context)
                } else {
                    return@of null
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