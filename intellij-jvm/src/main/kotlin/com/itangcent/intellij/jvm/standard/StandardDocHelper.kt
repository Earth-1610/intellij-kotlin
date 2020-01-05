package com.itangcent.intellij.jvm.standard

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.util.containers.stream
import com.itangcent.common.utils.append
import com.itangcent.common.utils.cast
import com.itangcent.common.utils.reduceSafely
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.ExtendProvider
import org.apache.commons.lang3.StringUtils
import java.util.*

@Singleton
open class StandardDocHelper : DocHelper {

    @Inject(optional = true)
    private val extendProvider: ExtendProvider? = null

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                val tags = ActionContext.getContext()!!.callInReadUI { docComment.findTagByName(tag) }
                return@let tags != null
            } ?: false
    }

    override fun findDocByTag(psiElement: PsiElement?, tag: String?): String? {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                return@let ActionContext.getContext()!!.callInReadUI {
                    val tags = docComment.findTagsByName(tag)
                    if (tags.isEmpty()) return@callInReadUI null
                    for (paramDocTag in tags) {
                        var result: String? = null
                        for (dataElement in paramDocTag.dataElements) {
                            val txt = dataElement.text?.trim()
                            if (txt.isNullOrBlank()) break
                            if (result == null) {
                                result = txt
                            } else {
                                result += txt
                            }
                        }
                        if (result != null) return@callInReadUI result
                    }
                    return@callInReadUI null
                }
            }
    }

    override fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>? {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                return@let ActionContext.getContext()!!.callInReadUI {

                    val tags = docComment.findTagsByName(tag)
                    if (tags.isEmpty()) return@callInReadUI null
                    val res: LinkedList<String> = LinkedList()
                    for (paramDocTag in tags) {
                        val data = paramDocTag.dataElements
                            .map { it?.text }
                            .filterNot { StringUtils.isBlank(it) }
                            .filterNotNull()
                            .map { it.trim() }
                            .reduceSafely { s1, s2 -> "$s1 $s2" }
                        if (!data.isNullOrEmpty()) {
                            res.add(data)
                        }
                    }
                    return@callInReadUI res
                }
            }
    }

    override fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String? {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                return@let ActionContext.getContext()!!.callInReadUI {
                    for (paramDocTag in docComment.findTagsByName(tag)) {

                        var n: String? = null
                        var value: String? = null

                        val elements = paramDocTag.dataElements
                            .asSequence()
                            .map { it?.text }
                            .filterNot { StringUtils.isBlank(it) }

                        loop@ for (element in elements) {
                            when {
                                n == null -> if (n == name) {
                                    n = element
                                } else {
                                    continue@loop
                                }
                                value == null -> value = element
                                else -> value += element
                            }
                        }

                        if (n == name) {
                            return@callInReadUI value
                        }
                    }
                    return@callInReadUI null
                }
            }
    }

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                return@let ActionContext.getContext()!!.callInReadUI {
                    return@callInReadUI getDocCommentContent(docComment)
                }
            }
    }

    override fun getDocCommentContent(docComment: PsiDocComment): String? {
        val descriptions = docComment.descriptionElements
        return descriptions.stream()
            .map { desc -> desc.text }
            ?.reduce { s1, s2 -> s1 + s2 }
            ?.map { it.trim() }
            ?.orElse(null)
    }

    override fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?> {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                return@let ActionContext.getContext()!!.callInReadUI {
                    val subTagMap: HashMap<String, String?> = HashMap()
                    for (paramDocTag in docComment.findTagsByName(tag)) {

                        var name: String? = null
                        var value: String? = null

                        val elements = paramDocTag.dataElements
                            .asSequence()
                            .map { it?.text }
                            .filterNot { StringUtils.isBlank(it) }

                        for (element in elements) {
                            when {
                                name == null -> name = element
                                value == null -> value = element
                                else -> value += element
                            }
                        }

                        if (name != null) {
                            subTagMap[name] = value
                        }
                    }
                    return@callInReadUI subTagMap
                }
            } ?: Collections.emptyMap()
    }

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {
        return psiElement.cast(PsiDocCommentOwner::class)?.docComment
            ?.let { docComment ->
                return@let ActionContext.getContext()!!.callInReadUI {
                    val tagMap: HashMap<String, String?> = HashMap()
                    docComment.tags.forEach { tag ->
                        tagMap[tag.name] = tag.dataElements
                            .mapNotNull { it?.text }
                            .filter { it.isNotBlank() }
                            .joinToString { it.trim() }
                    }
                    return@callInReadUI tagMap
                }
            } ?: Collections.emptyMap()
    }

    override fun getSuffixComment(psiElement: PsiElement): String? {

        val text = psiElement.text
        //text maybe null
        if (text == null) {
            return null
        }

        if (text.contains("//")) {
            return psiElement.children
                .filter { (it is PsiComment) && it.tokenType == JavaTokenType.END_OF_LINE_COMMENT }
                .map { it.text.trim() }
                .map { it.removePrefix("//") }
                .firstOrNull()
        }

        var nextSibling: PsiElement = psiElement
        while (true) {
            nextSibling = nextSibling.nextSibling ?: return null
            if (nextSibling is PsiWhiteSpace) {
                if (nextSibling.text?.contains('\n') ?: false) {
                    return null
                }
                continue
            }
            if (nextSibling is PsiComment) {
                break
            }
        }
        return (nextSibling as? PsiComment)?.text?.trim()?.removePrefix("//")
    }

    override fun getAttrOfField(field: PsiField): String? {

        val attrInDoc = getAttrOfDocComment(field)
        val suffixComment = getSuffixComment(field)
        val docByRule = extendProvider?.extraDoc(field)

        return attrInDoc.append(suffixComment).append(docByRule)
    }
}
