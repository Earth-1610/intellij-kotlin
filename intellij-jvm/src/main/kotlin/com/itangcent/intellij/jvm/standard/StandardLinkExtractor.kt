package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiMember
import com.itangcent.intellij.jvm.LinkExtractor
import com.itangcent.intellij.jvm.LinkResolver
import com.itangcent.intellij.jvm.PsiResolver
import java.util.regex.Pattern

@Singleton
open class StandardLinkExtractor : LinkExtractor {

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

    open fun findLink(
        doc: String,
        psiMember: PsiMember,
        resolver: (String) -> String?
    ): String {

        if (doc.contains("@link")) {
            val pattern = Pattern.compile("\\{@link (.*?)}")
            val matcher = pattern.matcher(doc)

            val sb = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(sb, "")
                val linkClassAndMethod = matcher.group(1)
                resolver(linkClassAndMethod)?.let { sb.append(it) }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        return doc
    }
}