package com.itangcent.intellij.config.rule

import com.intellij.psi.*

open class PsiFieldContext : PsiElementContext {

    protected var psiField: PsiField

    constructor(psiField: PsiField) {
        this.psiField = psiField
    }

    override fun getResource(): PsiElement {
        return psiField
    }

    override fun getName(): String? {
        return psiField.name
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiField
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner {
        return psiField
    }

    override fun asPsiMember(): PsiMember? {
        return psiField
    }

}
