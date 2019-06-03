package com.itangcent.intellij.config.context

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner

interface PsiElementContext {

    fun getResource(): PsiElement

    fun getName(): String?

    fun asPsiDocCommentOwner(): PsiDocCommentOwner
}
