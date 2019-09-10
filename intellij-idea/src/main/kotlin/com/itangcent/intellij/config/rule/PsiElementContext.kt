package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner

interface PsiElementContext {

    fun getResource(): PsiElement?

    fun getName(): String?

    /**
     * support doc comment
     */
    @Deprecated("use asPsiMember instead")
    fun asPsiDocCommentOwner(): PsiDocCommentOwner?

    /**
     * support annotation
     */
    @Deprecated("use asPsiMember instead")
    fun asPsiModifierListOwner(): PsiModifierListOwner?
}
