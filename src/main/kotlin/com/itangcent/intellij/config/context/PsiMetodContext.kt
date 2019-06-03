package com.itangcent.intellij.config.context

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner

class PsiMetodContext : PsiElementContext {

    private var psiMethod: PsiMethod

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
}
