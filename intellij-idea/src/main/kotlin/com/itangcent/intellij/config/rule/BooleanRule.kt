package com.itangcent.intellij.config.rule

interface BooleanRule : Rule<Boolean> {

    fun inverse(): BooleanRule {
        val origin = this
        return of { context ->
            origin.compute(context)?.let { !it }
        }
    }

    companion object {
        fun of(booleanRule: (PsiElementContext) -> Boolean?): BooleanRule {
            return object : BooleanRule {
                override fun compute(context: PsiElementContext): Boolean? {
                    return booleanRule(context)
                }
            }
        }
    }
}