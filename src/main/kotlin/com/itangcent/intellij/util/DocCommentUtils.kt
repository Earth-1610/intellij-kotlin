package com.itangcent.intellij.util

import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.util.containers.stream
import com.itangcent.intellij.context.ActionContext
import org.apache.commons.lang3.StringUtils
import java.util.*

object DocCommentUtils {

    fun hasTag(docComment: PsiDocComment?, tag: String?): Boolean {
        if (docComment != null) {
            val tags = ActionContext.getContext()!!.callInReadUI { docComment.findTagByName(tag) }
            return tags != null
        }
        return false
    }

    fun findDocByTag(docComment: PsiDocComment?, tag: String?): String? {
        if (docComment == null) {
            return null
        }
        return ActionContext.getContext()!!.callInReadUI {
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

    fun findDocsByTag(docComment: PsiDocComment?, tag: String?): String? {
        if (docComment == null) {
            return null
        }
        return ActionContext.getContext()!!.callInReadUI {
            val tags = docComment.findTagsByName(tag)
            if (tags.isEmpty()) return@callInReadUI null
            val res = StringBuilder()
            for (paramDocTag in tags) {
                paramDocTag.dataElements
                    .map { it?.text }
                    .filterNot { StringUtils.isBlank(it) }
                    .filterNotNull()
                    .map { it.trim() }
                    .forEach { res.append(it) }
            }
            return@callInReadUI res.toString()
        }
    }

    fun getAttrOfDocComment(docComment: PsiDocComment?): String? {
        if (docComment == null) {
            return null
        }
        return ActionContext.getContext()!!.callInReadUI {
            val descriptions = docComment.descriptionElements
            return@callInReadUI descriptions.stream()
                .map { desc -> desc.text }
                ?.reduce { s1, s2 -> s1 + s2 }
                ?.map { it.trim() }
                ?.orElse(null)
        }
    }

    fun getTagMapOfDocComment(docComment: PsiDocComment?): Map<String, String?> {
        if (docComment == null) {
            return Collections.emptyMap()
        }
        return ActionContext.getContext()!!.callInReadUI {
            val tagMap: HashMap<String, String?> = HashMap()
            docComment.tags.forEach { tag ->
                tagMap[tag.name] = tag.dataElements
                    .map { it?.text }
                    .filterNot { StringUtils.isBlank(it) }
                    .filterNotNull()
                    .map { it.trim() }
                    .reduceSafely { s1, s2 -> s1 + s2 }
            }
            return@callInReadUI tagMap
        } ?: Collections.emptyMap()
    }
}
