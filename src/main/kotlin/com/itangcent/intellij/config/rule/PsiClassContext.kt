package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner

open class PsiClassContext : PsiElementContext {
    protected var psiClass: PsiClass

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

    override fun asPsiModifierListOwner(): PsiModifierListOwner {
        return psiClass
    }

}
