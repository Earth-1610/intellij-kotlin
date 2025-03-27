package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.ExtendProvider
import com.itangcent.intellij.jvm.docComment
import java.util.*

/**
 * The standard single-line comment prefix used in most programming languages
 */
const val COMMENT_PREFIX = "//"

/**
 * Regular expression to match block comments in the format /* ... */
 * The pattern captures the content between /* and */, handling multiple asterisks
 */
val BLOCK_COMMENT_REGEX =
    Regex(
        "/\\*+(.*?)\\**/",
        setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)
    )

/**
 * List of common documentation comment prefixes that can appear in doc comments.
 * These prefixes are stripped when processing documentation text to get clean content.
 * Includes:
 * - "*" for Javadoc-style comments
 * - "///" for Markdown Documentation Comments (JEP 467)
 * - "//" for single-line comments
 */
val DOC_COMMENT_PREFIXES = listOf<String>(
    "*", "///", "//"
)

@Singleton
open class StandardDocHelper : DocHelper {

    @Inject(optional = true)
    private val extendProvider: ExtendProvider? = null

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {
        return psiElement.docComment { docComment ->
            val tags = docComment.findTagByName(tag)
            return@docComment tags != null
        } ?: false
    }

    override fun findDocByTag(psiElement: PsiElement?, tag: String?): String? {
        return psiElement.docComment { docComment ->
            val tags = docComment.findTagsByName(tag)
            if (tags.isEmpty()) return@docComment null
            for (paramDocTag in tags) {
                val text = paramDocTag.docValue()
                if (text.isNotBlank()) {
                    return@docComment text
                }
            }
            return@docComment ""
        }
    }

    private fun PsiDocTag.docValue(discardName: Boolean = false): String {
        val lines = this.text.lines()
        if (lines.isEmpty()) return ""
        var ret = lines[0].removePrefix(this.nameElement.text).trimStart()
        if (discardName) {
            this.valueElement?.text?.let {
                ret = ret.removePrefix(it).trimStart()
            }
        }
        if (lines.size == 1) {
            return ret
        }
        for (i in 1 until lines.size) {
            lines[i].trim()
                .removeCommentPrefix()
                .takeIf { it.isNotBlank() }
                ?.let {
                    ret = "$ret\n$it"
                }
        }
        return ret
    }

    /**
     * Removes any documentation comment prefix from the beginning of a line.
     * This is used to clean up documentation text by removing comment markers
     * like "*", "///", or "//" from the start of each line.
     * 
     * @return The line text with any matching prefix removed and trimmed
     */
    private fun String.removeCommentPrefix(): String {
        for (prefix in DOC_COMMENT_PREFIXES) {
            if (this.startsWith(prefix)) {
                return this.removePrefix(prefix).trim()
            }
        }
        return this
    }

    override fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>? {
        return psiElement.docComment { docComment ->
            val tags = docComment.findTagsByName(tag)
            if (tags.isEmpty()) return@docComment null
            val res: LinkedList<String> = LinkedList()
            for (paramDocTag in tags) {
                val data = paramDocTag.docValue()
                if (data.isNotBlank()) {
                    res.add(data)
                }
            }
            return@docComment res
        }
    }

    override fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String? {
        val tagAttr = "@$tag"
        return psiElement.docComment { docComment ->
            loopTags@ for (paramDocTag in docComment.findTagsByName(tag)) {
                if (paramDocTag.nameElement.text != tagAttr) {
                    continue@loopTags
                }
                if (paramDocTag.valueElement?.text != name) {
                    continue@loopTags
                }

                return@docComment paramDocTag.docValue(true)
            }
            return@docComment null
        }
    }

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {
        return psiElement.docComment { docComment ->
            getDocCommentContent(docComment)
        }
    }

    override fun getDocCommentContent(docComment: PsiDocComment): String? {
        val descriptions = docComment.descriptionElements
        val text = descriptions.joinToString(separator = "") { desc -> desc.tinyText() }.trim()
        val lines = text.lines()
        return lines.asSequence()
            .map { it.removePrefix(" ").trimEnd() }
            .joinToString(separator = "\n")
            .trim()
    }

    private fun PsiElement.tinyText(): String {
        val text = this.text
        if (text.notNullOrBlank()) {
            return text.trimEnd()
        }
        if (text.contains("\n")) {
            return "\n"
        }
        return text
    }

    override fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?> {
        return psiElement.docComment { docComment ->
            val subTagMap: HashMap<String, String?> = HashMap()
            for (paramDocTag in docComment.findTagsByName(tag)) {
                paramDocTag.valueElement?.text?.let {
                    subTagMap[it] = paramDocTag.docValue(true)
                }
            }
            return@docComment subTagMap
        } ?: Collections.emptyMap()
    }

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {
        return psiElement.docComment { docComment ->
            val tagMap: HashMap<String, String?> = HashMap()
            docComment.tags.forEach { tag ->
                tagMap[tag.name] = tag.docValue()
            }
            return@docComment tagMap
        } ?: Collections.emptyMap()
    }

    override fun getEolComment(psiElement: PsiElement): String? {

        //text maybe null
        val text = psiElement.text ?: return null

        if (text.contains(COMMENT_PREFIX)) {
            return psiElement.children
                .asSequence()
                .findEolComment { false }
        }

        psiElement.nextSiblings()
            .findEolComment()
            ?.let {
                return it
            }

        return null
    }

    override fun getAttrOfField(field: PsiField): String? {

        val attrInDoc = getAttrOfDocComment(field)
        val eolComment = getEolComment(field)?.takeIf { it != attrInDoc }
        val docByRule = extendProvider?.extraDoc(field)

        return listOfNotNull(attrInDoc, eolComment, docByRule)
            .distinct()
            .joinToString("\n")
    }

    open fun Sequence<PsiElement>.findEolComment(
        stopInUnexpectedElement: (PsiElement) -> Boolean = { true }
    ): String? {
        for (next in this) {
            when {
                next.isWhiteSpace() -> {
                    if (next.text?.contains('\n') == true) {
                        return null
                    }
                    continue
                }

                next is PsiComment -> {
                    return next.eolComment()
                }
            }
            if (stopInUnexpectedElement(next)) {
                return null
            }
        }

        return null
    }

    open fun Sequence<PsiElement>.findBlockComment(
        stopInUnexpectedElement: (PsiElement) -> Boolean = { true }
    ): String? {
        for (next in this) {
            when {
                next.isWhiteSpace() -> {
                    continue
                }

                next is PsiComment -> {
                    return next.blockComment()
                }

                stopInUnexpectedElement(next) -> {
                    return null
                }
            }
        }

        return null
    }

    open fun PsiElement.isWhiteSpace() = this is PsiWhiteSpace

    open fun PsiComment.eolComment(): String? {
        if (this.tokenType == JavaTokenType.END_OF_LINE_COMMENT) {
            return text.trim().removePrefix(COMMENT_PREFIX).trimStart()
        }
        return null
    }

    open fun PsiComment.blockComment(): String? {
        if (this.tokenType == JavaTokenType.C_STYLE_COMMENT) {
            return BLOCK_COMMENT_REGEX.find(text)?.let { it.groupValues[1].trim() }
        }
        return null
    }

    protected fun PsiElement.prevSiblings() = generateSequence { it.prevSibling }
    protected fun PsiElement.nextSiblings() = generateSequence { it.nextSibling }

    protected fun PsiElement.generateSequence(nextFunction: (PsiElement) -> PsiElement?): Sequence<PsiElement> {
        return generateSequence(nextFunction(this), nextFunction)
    }
}
