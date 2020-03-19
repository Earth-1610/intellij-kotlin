package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.intellij.jvm.element.ExplicitClass

open class PsiClassContext : RuleContext {

    protected var psiClass: PsiClass

    constructor(psiClass: PsiClass) {
        this.psiClass = psiClass
    }

    override fun getResource(): PsiElement {
        return psiClass
    }

    override fun getName(): String? {
        return psiClass.qualifiedName
    }

    override fun getSimpleName(): String? {
        return psiClass.name
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiClass
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiClass
    }
}


class ExplicitClassContext : PsiClassContext {
    private var explicitClass: ExplicitClass

    constructor(explicitClass: ExplicitClass) : super(explicitClass.psi()) {
        this.explicitClass = explicitClass
    }

    override fun getCore(): Any? {
        return explicitClass
    }
}