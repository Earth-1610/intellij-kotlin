package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.util.containers.stream
import com.itangcent.common.utils.appendln
import com.itangcent.common.utils.firstOrNull
import com.itangcent.common.utils.joinToString
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.ExtendProvider
import com.itangcent.intellij.jvm.docComment
import java.util.*

const val COMMENT_PREFIX = "//"

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
                .removePrefix("*")
                .takeIf { it.isNotBlank() }
                ?.let {
                    ret += '\n'
                    ret += it
                }
        }
        return ret
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
        if (lines.size > 1 && lines.stream().skip(1)
                .filter { it.startsWith(" ") }.count() >= lines.size / 2
        ) {
            return lines[0] + "\n" + lines.stream().skip(1)
                .map { it.removePrefix(" ") }
                .joinToString(separator = "\n")
        }
        return text
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

    override fun getSuffixComment(psiElement: PsiElement): String? {

        //text maybe null
        val text = psiElement.text ?: return null

        if (text.contains(COMMENT_PREFIX)) {
            return psiElement.children
                .stream()
                .filter { (it is PsiComment) && it.tokenType == JavaTokenType.END_OF_LINE_COMMENT }
                .map { it.text.trim() }
                .map { it.removePrefix(COMMENT_PREFIX) }
                .firstOrNull()
        }

        var nextSibling: PsiElement = psiElement
        while (true) {
            nextSibling = nextSibling.nextSibling ?: return null
            if (nextSibling is PsiWhiteSpace) {
                if (nextSibling.text?.contains('\n') == true) {
                    return null
                }
                continue
            }
            if (nextSibling is PsiComment) {
                break
            }
        }
        return (nextSibling as? PsiComment)?.text?.trim()?.removePrefix(COMMENT_PREFIX)
    }

    override fun getAttrOfField(field: PsiField): String? {

        val attrInDoc = getAttrOfDocComment(field)
        val suffixComment = getSuffixComment(field)
        val docByRule = extendProvider?.extraDoc(field)

        return attrInDoc.appendln(suffixComment).appendln(docByRule)
    }
}
