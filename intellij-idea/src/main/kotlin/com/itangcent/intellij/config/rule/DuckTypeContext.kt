package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.intellij.jvm.duck.DuckType

open class DuckTypeContext : RuleContext {

    protected var duckType: DuckType

    protected var psiClass: PsiClass?

    constructor(duckType: DuckType, psiClass: PsiClass?) {
        this.duckType = duckType
        this.psiClass = psiClass
    }

    override fun getCore(): Any? {
        return duckType
    }

    override fun getResource(): PsiElement? {
        return psiClass
    }

    override fun getName(): String? {
        return duckType.canonicalText()
    }

    override fun getSimpleName(): String? {
        return duckType.name()
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner? {
        return psiClass
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiClass
    }
}
