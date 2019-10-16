package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifierListOwner

open class PsiFieldContext : RuleContext {

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
}
