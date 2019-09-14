package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.itangcent.intellij.jvm.standard.StandardPsiResolver

@ImplementedBy(StandardPsiResolver::class)
interface PsiResolver {

    fun resolveClass(className: String, psiMember: PsiMember): PsiClass?

    fun resolveClassWithPropertyOrMethod(classNameWithProperty: String, psiMember: PsiMember): Pair<PsiClass?, PsiElement?>?

    fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement?

    fun getContainingClass(psiMember: PsiMember): PsiClass?

    fun resolveRefText(psiExpression: PsiElement?): String?
}