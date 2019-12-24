package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.itangcent.common.utils.cast
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.DocHelper
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocTokenType
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.ScDocComment

class ScalaDocHelper(private val docHelper: DocHelper) : DocHelper by docHelper {

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {

        if (psiElement == null) {
            return null
        }

        if (ScPsiUtils.isScPsiInst(psiElement)) {
            val content = psiElement.cast(PsiDocCommentOwner::class)?.docComment
                ?.let { docComment ->
                    return@let ActionContext.getContext()!!.callInReadUI {
                        return@callInReadUI getDocCommentContent(docComment)
                    }
                }
            if (!content.isNullOrBlank()) {
                return content
            }
        }

        return docHelper.getAttrOfDocComment(psiElement)
    }

    override fun getDocCommentContent(docComment: PsiDocComment): String? {

        val docCommentContent = docHelper.getDocCommentContent(docComment)
        if (!docCommentContent.isNullOrBlank()) {
            return docCommentContent
        }

        if (docComment is ScDocComment) {
            return docComment.children
                .filter { it is LeafPsiElement }
                .map { it as LeafPsiElement }
                .filter { it.elementType == ScalaDocTokenType.DOC_COMMENT_DATA }
                .joinToString(separator = " ") { it.text }
        }

        return docCommentContent
    }
}