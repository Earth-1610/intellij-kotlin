package com.itangcent.intellij.config.rule

import com.intellij.psi.*

open class PsiMethodContext : PsiElementContext {

    protected var psiMethod: PsiMethod

    constructor(psiMethod: PsiMethod) {
        this.psiMethod = psiMethod
    }

    override fun getResource(): PsiElement {
        return psiMethod
    }

    override fun getName(): String? {
        return psiMethod.name
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiMethod
    }

    override fun asPsiMember(): PsiMember? {
        return psiMethod
    }

}
