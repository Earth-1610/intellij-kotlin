package com.itangcent.intellij.jvm.groovy

import com.google.inject.Singleton
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.itangcent.intellij.jvm.standard.BLOCK_COMMENT_REGEX
import com.itangcent.intellij.jvm.standard.COMMENT_PREFIX
import com.itangcent.intellij.jvm.standard.StandardDocHelper
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementTypes

@Singleton
class GrDocHelper : StandardDocHelper() {

    override fun getEolComment(psiElement: PsiElement): String? {
        GrPsiUtils.assertGrPsiInst(psiElement)

        psiElement.parent.prevSiblings()
            .findBlockComment()
            ?.let {
                return it
            }

        psiElement.parent.nextSiblings()
            .findEolComment { !it.isSemicolon() }
            ?.let {
                return it
            }

        return null
    }

    override fun hasTag(psiElement: PsiElement?, tag: String?): Boolean {
        throw NotImplementedError()
    }

    override fun findDocByTag(psiElement: PsiElement?, tag: String?): String? {
        throw NotImplementedError()
    }

    override fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>? {
        throw NotImplementedError()
    }

    override fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String? {
        throw NotImplementedError()
    }

    override fun getAttrOfDocComment(psiElement: PsiElement?): String? {
        if (psiElement == null) {
            return null
        }
        GrPsiUtils.assertGrPsiInst(psiElement)
        return super.getAttrOfDocComment(psiElement)
    }

    override fun getDocCommentContent(docComment: PsiDocComment): String? {
        GrPsiUtils.assertGrPsiInst(docComment)
        return super.getDocCommentContent(docComment)
    }

    override fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?> {
        throw NotImplementedError()
    }

    override fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?> {
        throw NotImplementedError()
    }

    override fun getAttrOfField(field: PsiField): String? {
        GrPsiUtils.assertGrPsiInst(field)
        return super.getAttrOfField(field)
    }

    override fun PsiComment.eolComment(): String? {
        if (this.tokenType == GroovyElementTypes.SL_COMMENT) {
            return text.trim().removePrefix(COMMENT_PREFIX).trimStart()
        }
        return null
    }

    override fun PsiComment.blockComment(): String? {
        if (this.tokenType == GroovyElementTypes.ML_COMMENT) {
            return BLOCK_COMMENT_REGEX.find(text)?.let { it.groupValues[1].trim() }
        }
        return null
    }

    override fun PsiElement.isWhiteSpace(): Boolean {
        return this.isNewLine() || this is PsiWhiteSpace
    }

    private fun PsiElement.isSemicolon(): Boolean =
        this is LeafPsiElement && this.elementType == GroovyElementTypes.T_SEMI

    private fun PsiElement.isNewLine(): Boolean =
        this is LeafPsiElement && this.elementType == GroovyElementTypes.NL
}
