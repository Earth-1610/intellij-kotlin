package com.itangcent.intellij.config.context

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField

class PsiFieldContext : PsiElementContext {

    private var psiField: PsiField

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
}
