package com.itangcent.intellij.jvm.groovy

import com.google.inject.Singleton
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.itangcent.intellij.jvm.standard.BLOCK_COMMENT_REGEX
import com.itangcent.intellij.jvm.standard.COMMENT_PREFIX
import com.itangcent.intellij.jvm.standard.StandardDocHelper
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementTypes
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField

@Singleton
class GrDocHelper : StandardDocHelper() {

    override fun getEolComment(psiElement: PsiElement): String? {
        if (psiElement !is GrField) {
            return null
        }

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
