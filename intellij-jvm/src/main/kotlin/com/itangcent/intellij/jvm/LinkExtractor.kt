package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.itangcent.intellij.jvm.standard.StandardLinkExtractor

@ImplementedBy(StandardLinkExtractor::class)
interface LinkExtractor {

    fun extract(
        doc: String?,
        psiElement: PsiElement,
        resolve: LinkResolver
    ): String?

}