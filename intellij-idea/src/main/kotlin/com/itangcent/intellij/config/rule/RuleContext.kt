package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.Extensible

interface RuleContext : Extensible {

    fun getCore(): Any? {
        return getResource()
    }

    fun getPsiContext(): PsiElement? {
        return getResource()
    }

    fun getResource(): PsiElement?

    fun getName(): String?

    fun getSimpleName(): String? {
        return getName()
    }

    /**
     * support doc comment
     */
    fun asPsiDocCommentOwner(): PsiDocCommentOwner?

    /**
     * support annotation
     */
    fun asPsiModifierListOwner(): PsiModifierListOwner?
}
