package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.getPropertyValue

open class UnknownPsiElementContext : RuleContext {

    private var psiElement: PsiElement

    constructor(psiElement: PsiElement) {
        this.psiElement = psiElement
    }

    override fun getResource(): PsiElement {
        return psiElement
    }

    override fun getName(): String? {
        return psiElement.getPropertyValue("name")?.toString()
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner? {
        return psiElement as? PsiDocCommentOwner
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiElement as? PsiModifierListOwner?
    }

}
