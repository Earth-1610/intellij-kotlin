package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.intellij.psi.PsiMember
import com.itangcent.intellij.jvm.LinkExtractor
import com.itangcent.intellij.jvm.LinkResolver
import com.itangcent.intellij.jvm.PsiResolver

abstract class AbstractLinkExtractor : LinkExtractor {

    @Inject
    protected val psiResolver: PsiResolver? = null

    override fun extract(
        doc: String?,
        psiMember: PsiMember,
        resolve: LinkResolver
    ): String? {

        if (doc.isNullOrBlank()) return doc

        return findLink(doc, psiMember) { linkClassAndMethod ->
            resolve.linkToPsiElement(
                linkClassAndMethod,
                psiResolver!!.resolveClassWithPropertyOrMethod(linkClassAndMethod, psiMember)?.let {
                    it.second ?: it.first
                }
            )
        }
    }

    abstract fun findLink(doc: String, psiMember: PsiMember, resolver: (String) -> String?): String
}