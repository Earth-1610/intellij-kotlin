package com.itangcent.intellij.psi

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.itangcent.intellij.jvm.duck.DuckType


@ImplementedBy(DefaultClassRuleConfig::class)
interface ClassRuleConfig {

    /**
     * try convert one class to another for parse
     */
    fun tryConvert(psiType: PsiType, context: PsiElement? = null): PsiType

    /**
     * try convert one class to another for parse
     */
    fun tryConvert(duckType: DuckType, context: PsiElement? = null): DuckType

    /**
     * try convert one class to another for parse
     */
    fun tryConvert(psiClass: PsiClass): PsiClass

    /**
     * max parsing deep to prevent the stack overflow occurring.
     */
    fun maxDeep(): Int

    /**
     * max parsing elements to prevent the stack overflow occurring.
     */
    fun maxElements(): Int
}