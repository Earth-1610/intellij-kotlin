package com.itangcent.intellij.util

import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.util.containers.stream
import org.apache.commons.lang3.StringUtils

object DocCommentUtils {

    fun hasTag(docComment: PsiDocComment?, tag: String?): Boolean {
        if (docComment != null) {
            val tags = docComment.findTagByName(tag)
            return tags != null
        }
        return false
    }

    fun findDocByTag(docComment: PsiDocComment?, tag: String?): String? {
        if (docComment != null) {
            val tags = docComment.findTagsByName(tag)
            if (tags.isEmpty()) return null
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
                if (result != null) return result
            }
        }
        return null
    }

    fun findDocsByTag(docComment: PsiDocComment?, tag: String?): String? {
        if (docComment != null) {
            val tags = docComment.findTagsByName(tag)
            if (tags.isEmpty()) return null
            val res = StringBuilder()
            for (paramDocTag in tags) {
                paramDocTag.dataElements
                    .map { it?.text }
                    .filterNot { StringUtils.isBlank(it) }
                    .filterNotNull()
                    .map { it.trim() }
                    .forEach { res.append(it) }
            }
            return res.toString()
        }
        return null
    }

    fun getAttrOfDocComment(docComment: PsiDocComment?): String? {
        val descriptions = docComment?.descriptionElements
        return descriptions
            ?.stream()
            ?.map { desc -> desc.text }
            ?.map { it.trim() }
            ?.reduce { s1, s2 -> s1 + "\n" + s2 }
            ?.map { it.trim() }
            ?.orElse(null)
    }

    fun getTagMapOfDocComment(docComment: PsiDocComment?): Map<String, String?> {

        val tagMap: HashMap<String, String?> = HashMap()
        docComment?.tags?.forEach { tag ->
            tagMap[tag.name] = tag.dataElements
                .map { it?.text }
                .filterNot { StringUtils.isBlank(it) }
                .filterNotNull()
                .map { it.trim() }
                .reduceSafely { s1, s2 -> s1 + " " + s2 }
        }
        return tagMap
    }
}
