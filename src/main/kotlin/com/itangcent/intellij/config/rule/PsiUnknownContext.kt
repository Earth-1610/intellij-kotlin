package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.intellij.util.getPropertyValue

open class PsiUnknownContext : PsiElementContext {

    private var psiElement: PsiElement

    constructor(psiClass: PsiElement) {
        this.psiElement = psiClass
    }

    override fun getResource(): PsiElement {
        return psiElement
    }

    override fun getName(): String? {
        return psiElement.getPropertyValue("name")?.toString()
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner? {
        if (psiElement is PsiDocCommentOwner) {
            return psiElement as PsiDocCommentOwner
        }
        return null
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        if (psiElement is PsiModifierListOwner) {
            return psiElement as PsiModifierListOwner
        }
        return null
    }
}
