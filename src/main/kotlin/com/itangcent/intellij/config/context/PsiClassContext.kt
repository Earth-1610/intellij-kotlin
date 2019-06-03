package com.itangcent.intellij.config.context

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement

class PsiClassContext : PsiElementContext {

    private var psiClass: PsiClass

    constructor(psiClass: PsiClass) {
        this.psiClass = psiClass
    }

    override fun getResource(): PsiElement {
        return psiClass
    }

    override fun getName(): String? {
        return psiClass.name
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiClass
    }
}
