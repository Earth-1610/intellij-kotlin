package com.itangcent.intellij.jvm.standard

import com.google.inject.Singleton
import com.intellij.psi.PsiMember
import java.util.regex.Pattern

@Singleton
open class StandardLinkExtractor : AbstractLinkExtractor() {

    override fun findLink(
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