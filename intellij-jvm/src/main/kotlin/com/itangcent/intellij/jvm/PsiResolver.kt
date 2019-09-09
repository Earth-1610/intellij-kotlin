package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.standard.StandardPsiResolver

@ImplementedBy(StandardPsiResolver::class)
interface PsiResolver {

    fun resolveRefText(psiExpression: PsiElement?): String?
}