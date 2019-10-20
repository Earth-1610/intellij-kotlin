package com.itangcent.intellij.psi

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType


@ImplementedBy(DefaultClassRuleConfig::class)
interface ClassRuleConfig {

    /**
     * try convert one class to another for parse
     */
    fun tryConvert(psiType: PsiType, context: PsiElement? = null): PsiType

    /**
     * try convert one class to another for parse
     */
    fun tryConvert(psiClass: PsiClass): PsiClass
}