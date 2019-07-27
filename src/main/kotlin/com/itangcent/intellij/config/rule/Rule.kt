package com.itangcent.intellij.config.rule

interface Rule<T> {
    fun compute(context: PsiElementContext): T?
}