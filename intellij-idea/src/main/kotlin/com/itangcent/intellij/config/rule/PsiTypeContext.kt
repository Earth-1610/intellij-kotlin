package com.itangcent.intellij.config.rule

import com.intellij.psi.*
import com.itangcent.common.utils.SimpleExtensible

open class PsiTypeContext : SimpleExtensible, RuleContext {

    protected var psiType: PsiType

    protected var psiClass: PsiClass?

    constructor(psiType: PsiType, psiClass: PsiClass?) {
        this.psiType = psiType
        this.psiClass = psiClass
    }

    override fun getCore(): Any? {
        return psiType
    }

    override fun getResource(): PsiElement? {
        return psiClass
    }

    override fun getName(): String? {
        return psiType.canonicalText
    }

    override fun getSimpleName(): String? {
        return psiType.presentableText
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner? {
        return psiClass
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiClass
    }
}
