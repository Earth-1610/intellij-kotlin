package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.LinkExtractor
import com.itangcent.intellij.jvm.LinkResolver
import com.itangcent.intellij.jvm.PsiResolver

abstract class AbstractLinkExtractor : LinkExtractor {

    @Inject
    protected val psiResolver: PsiResolver? = null

    override fun extract(
        doc: String?,
        psiElement: PsiElement,
        resolve: LinkResolver
    ): String? {

        if (doc.isNullOrBlank()) return doc

        return findLink(doc, psiElement) { linkClassAndMethod ->
            resolve.linkToPsiElement(
                linkClassAndMethod,
                psiResolver!!.resolveClassWithPropertyOrMethod(linkClassAndMethod, psiElement)?.let {
                    it.second ?: it.first
                }
            )
        }
    }

    abstract fun findLink(doc: String, psiElement: PsiElement, resolver: (String) -> String?): String
}