package com.itangcent.intellij.config.rule

interface StringRule : Rule<String> {
    companion object {
        fun of(stringRule: (RuleContext) -> String?): StringRule {
            return object : StringRule {
                override fun compute(context: RuleContext): String? {
                    return stringRule(context)
                }
            }
        }

        fun filterWith(boolRule: BooleanRule, stringRule: StringRule): StringRule {
            return of { context ->
                if (boolRule.compute(context) == true) {
                    return@of stringRule.compute(context)
                } else {
                    return@of null
                }
            }
        }
    }
}