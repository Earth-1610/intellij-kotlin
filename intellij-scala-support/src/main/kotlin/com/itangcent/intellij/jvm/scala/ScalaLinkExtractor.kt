package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.standard.AbstractLinkExtractor
import java.util.regex.Pattern

/**
 * see https://docs.scala-lang.org/style/scaladoc.html
 */
open class ScalaLinkExtractor : AbstractLinkExtractor() {

    override fun findLink(doc: String, psiElement: PsiElement, resolver: (String) -> String?): String {

        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        if (doc.contains("@link") || doc.contains("[[")) {
            val pattern = Pattern.compile("\\{@link(.*?)}|\\[\\[(.*?)]]")
            val matcher = pattern.matcher(doc)

            val sb = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(sb, "")
                val linkClassAndMethod = matcher.group(1)
                    ?: matcher.group(2)
                    ?: continue
                resolver(linkClassAndMethod)?.let { sb.append(it) }
            }
            matcher.appendTail(sb)
            return sb.toString()
        }

        return doc
    }
}