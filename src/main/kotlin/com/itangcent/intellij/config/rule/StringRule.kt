package com.itangcent.intellij.config.rule

interface StringRule : Rule<String> {
    companion object {
        fun of(stringRule: (PsiElementContext) -> String?): StringRule {
            return object : StringRule {
                override fun compute(context: PsiElementContext): String? {
                    return stringRule(context)
                }
            }
        }
    }
}