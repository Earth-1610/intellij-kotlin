package com.itangcent.intellij.jvm.standard

import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import java.util.regex.Pattern

@Singleton
open class StandardLinkExtractor : AbstractLinkExtractor() {

    override fun findLink(
        doc: String,
        psiElement: PsiElement,
        resolver: (String) -> String?
    ): String {
        // First collect reference definitions if any
        val referenceDefinitions = mutableMapOf<String, String>()
        if (doc.containsMarkdownLinks()) {
            val refPattern = Pattern.compile("\\[([^]]+)]:\\s*(.+)$", Pattern.MULTILINE)
            val refMatcher = refPattern.matcher(doc)
            while (refMatcher.find()) {
                val ref = refMatcher.group(1)
                val url = refMatcher.group(2)
                referenceDefinitions[ref] = url
            }
        }

        // Combined pattern for all types of links
        val combinedPattern = Pattern.compile(
            "\\{@link (.*?)}|" + // @link tags
                    "\\[([^]]+)]\\(([^)]+)\\)|" + // inline links [text](url)
                    "\\[([^]]+)]\\[([^]]+)]|" + // reference links [text][ref]
                    "\\[([^]]+)]|" + // collapsed reference links [text]
                    "`([^`]+)`" // backtick-wrapped text
        )

        val matcher = combinedPattern.matcher(doc)
        val sb = StringBuffer()

        while (matcher.find()) {
            matcher.appendReplacement(sb, "")

            // Handle @link tags
            if (matcher.group(1) != null) {
                val linkClassAndMethod = matcher.group(1)
                resolver(linkClassAndMethod)?.let { sb.append(it) } ?: sb.append(matcher.group(0))
            }
            // Handle inline link [text](url)
            else if (matcher.group(2) != null && matcher.group(3) != null) {
                val text = matcher.group(2)
                val url = matcher.group(3)
                resolver(url)?.let { sb.append(it) } ?: sb.append(matcher.group(0))
            }
            // Handle reference link [text][ref]
            else if (matcher.group(4) != null && matcher.group(5) != null) {
                val text = matcher.group(4)
                val ref = matcher.group(5)
                val url = referenceDefinitions[ref] ?: ref
                resolver(url)?.let { sb.append(it) } ?: sb.append(matcher.group(0))
            }
            // Handle collapsed reference link [text][] or bare [text]
            else if (matcher.group(6) != null) {
                val text = matcher.group(6)
                val url = referenceDefinitions[text] ?: text
                resolver(url)?.let { sb.append(it) } ?: sb.append(matcher.group(0))
            }
            // Handle backtick-wrapped text
            else if (matcher.group(7) != null) {
                val text = matcher.group(7)
                resolver(text)?.let { sb.append(it) } ?: sb.append(matcher.group(0))
            }
        }

        matcher.appendTail(sb)
        return sb.toString()
    }

    fun String.containsMarkdownLinks(): Boolean = contains("[") && contains("]")
}