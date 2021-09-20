package com.itangcent.intellij.config.rule

interface Rule<T> {
    fun compute(context: RuleContext): T?

    fun filterWith(filter: Rule<Boolean>): Rule<T> {
        return object : Rule<T> {
            override fun compute(context: RuleContext): T? {
                return if (filter.compute(context) == true) {
                    this.compute(context)
                } else {
                    null
                }
            }
        }
    }
}