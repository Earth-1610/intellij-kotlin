package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.itangcent.intellij.jvm.standard.StandardPsiExpressionResolver
import kotlin.reflect.KClass

@ImplementedBy(StandardPsiExpressionResolver::class)
interface PsiExpressionResolver {

    fun process(psiElement: PsiElement): Any?

    fun process(psiExpression: PsiExpression): Any?

    fun processStaticField(psiField: PsiField): Any?

    fun <T : Any> registerExpressionResolver(cls: KClass<T>, handle: (T) -> Any?)

    fun <T : Any> registerExpressionResolver(predicate: (Any) -> Boolean, handle: (T) -> Any?)
}