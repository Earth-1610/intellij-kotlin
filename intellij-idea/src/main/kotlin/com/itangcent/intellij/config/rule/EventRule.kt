package com.itangcent.intellij.config.rule

interface EventRule : Rule<Unit> {

    companion object {
        fun of(eventRule: (RuleContext) -> Unit): EventRule {
            return object : EventRule {
                override fun compute(context: RuleContext): Unit {
                    return eventRule(context)
                }
            }
        }
    }
}