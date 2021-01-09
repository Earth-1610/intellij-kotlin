package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiType
import com.itangcent.intellij.jvm.standard.StandardPsiResolver

@ImplementedBy(StandardPsiResolver::class)
interface PsiResolver {

    fun resolveClass(className: String, context: PsiElement): PsiClass?

    /**
     * @return [PsiClass]|[com.intellij.psi.PsiType]
     */
    fun resolveClassOrType(className: String, context: PsiElement): Any?

    /**
     * @return [Pair]<[PsiClass]|[com.intellij.psi.PsiType],[PsiElement]>
     */
    fun resolveClassWithPropertyOrMethod(
        classNameWithProperty: String,
        context: PsiElement
    ): Pair<Any?, PsiElement?>?

    fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement?

    fun getContainingClass(psiElement: PsiElement): PsiClass?

    fun resolveRefText(psiExpression: PsiElement?): String?

    /**
     * return
     * {
     * params:{}
     * name:""
     * ordinal:1
     * desc:""
     * }
     */
    fun resolveEnumFields(index: Int, psiField: PsiField): Map<String, Any?>?

    fun findClass(fqClassName: String, context: PsiElement): PsiClass?

    fun findType(canonicalText: String, context: PsiElement): PsiType?

    fun visit(psiElement: Any, visitor: (Any) -> Unit)
}