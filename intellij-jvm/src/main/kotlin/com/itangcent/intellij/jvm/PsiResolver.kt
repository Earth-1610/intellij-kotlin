package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.standard.StandardPsiResolver
import kotlin.reflect.KClass

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

    fun resolveRefText(psiElement: PsiElement?): String?

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

    fun getChildren(psiElement: PsiElement): Array<PsiElement>

    fun getReturnType(psiMethod: PsiMethod): PsiType?

    fun <T : PsiElement> getContextOfType(element: PsiElement, vararg classes: KClass<out T>): T?
}

fun PsiMethod.getResolvedReturnType(): PsiType? {
    return ActionContext.getContext()?.instance(PsiResolver::class)
        ?.getReturnType(this) ?: this.returnType
}