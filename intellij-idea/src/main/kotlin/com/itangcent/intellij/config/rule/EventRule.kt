package com.itangcent.intellij.config.rule

interface EventRule : Rule<Unit> {
    override fun filterWith(filter: Rule<Boolean>): Rule<Unit> {
        return of { context ->
            if (filter.compute(context) == true) {
                this.compute(context)
            }
        }
    }

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