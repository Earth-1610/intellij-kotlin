package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.itangcent.common.utils.append
import com.itangcent.common.utils.appendln
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.scala.adaptor.scalaAdaptor
import com.itangcent.intellij.jvm.standard.StandardDocHelper
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocComment
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocSyntaxElement
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocTag
import java.util.*

class ScalaDocHelper : StandardDocHelper() {

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?> {

        if (psiElement == null) {
            return Collections.emptyMap()
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            return (psiElement as? PsiDocCommentOwner)?.docComment
                ?.let { docComment ->
                    return@let ActionContext.getContext()!!.callInReadUI {
                        val subTagMap: HashMap<String, String?> = HashMap()
                        docComment.tags
                            .filter { it.name == "@$tag" }
                            .forEach {
                                subTagMap[it.valueElement?.text ?: ""] =
                                    (it as? ScDocTag)?.scalaAdaptor()?.getAllText()
                            }
                        return@callInReadUI subTagMap
                    }
                } ?: Collections.emptyMap()
        }

        return Collections.emptyMap()
    }

    override fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String? {

        if (psiElement == null) {
            return null
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            return (psiElement as? PsiDocCommentOwner)?.docComment
                ?.let { docComment ->
                    return@let ActionContext.getContext()!!.callInReadUI {
                        return@callInReadUI docComment.tags
                            .filter { it.name == "@$tag" }
                            .filter { it.valueElement!!.text == name }
                            .map { (it as? ScDocTag)?.scalaAdaptor()?.getAllText() }
                            .firstOrNull()
                    }
                }
        }

        return null
    }

    override fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>? {

        if (psiElement == null) {
            return null
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            return (psiElement as? PsiDocCommentOwner)?.docComment
                ?.let { docComment ->
                    return@let ActionContext.getContext()!!.callInReadUI {
                        docComment.tags
                            .filter { it.name == "@$tag" }
                            .map { (it as? ScDocTag)?.scalaAdaptor()?.getAllText() }
                            .filter { !it.isNullOrBlank() }
                            .map { it!! }
                            .toList()

                    }
                }
        }

        return null
    }

    override fun findDocByTag(psiElement: PsiElement?, tag: String?): String? {

        if (psiElement == null) {
            return null
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {

            return (psiElement as? PsiDocCommentOwner)
                ?.docComment
                ?.let { docComment ->
                    return docComment.tags.filter { it.name == "@$tag" }
                        .map { (it as? ScDocTag)?.scalaAdaptor()?.getAllText() }
                        .firstOrNull()
                }
        }

        return null
    }

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {

        if (psiElement == null) {
            return false
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {

            return (psiElement as? PsiDocCommentOwner)
                ?.docComment
                ?.let { docComment ->
                    return docComment.tags.any { it.name == "@$tag" }
                } ?: false
        }

        return false
    }

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {

        if (psiElement == null) {
            return null
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            val content = (psiElement as? PsiDocCommentOwner)?.docComment
                ?.let { docComment ->
                    return@let ActionContext.getContext()!!.callInReadUI {
                        return@callInReadUI getDocCommentContent(docComment)
                    }
                }
            if (!content.isNullOrBlank()) {
                return content
            }
        }

        return null
    }

    override fun getDocCommentContent(docComment: PsiDocComment): String? {

        if (docComment is ScDocComment) {
            var str: String? = null
            for (child in docComment.children) {
                if (child is LeafPsiElement) {
                    if (child.elementType == ScalaDocTokenType.DOC_COMMENT_DATA) {
                        child.text.trim()
                            .takeIf { it.isNotBlank() }
                            ?.let { str = str.append(it) }
                    } else if (child.elementType == ScalaDocTokenType.DOC_WHITESPACE) {
                        if (child.text.contains('\n')) {
                            str = str.appendln()
                        }
                    }
                } else if (child is ScDocSyntaxElement) {
                    child.text.trim()
                        .takeIf { it.isNotBlank() }
                        ?.let { str = str.append(it) }
                }
            }
            return str?.trim()
        }

        return null
    }

    override fun getEolComment(psiElement: PsiElement): String? {

        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        return super.getEolComment(psiElement)
    }

    override fun getAttrOfField(field: PsiField): String? {

        if (!ScPsiUtils.isScPsiInst(field)) {
            throw NotImplementedError()
        }

        return super.getAttrOfField(field)
    }
}