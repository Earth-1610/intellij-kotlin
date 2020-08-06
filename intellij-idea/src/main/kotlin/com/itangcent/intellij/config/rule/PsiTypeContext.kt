package com.itangcent.intellij.config.rule

import com.intellij.psi.*
import com.itangcent.common.utils.SimpleExtensible
import com.itangcent.common.utils.cache

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
        return this.cache("_name") { psiType.canonicalText }
    }

    override fun getSimpleName(): String? {
        return this.cache("_simpleName") { psiType.presentableText }
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner? {
        return psiClass
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiClass
    }

    override fun toString(): String {
        return getName() ?: "anonymous"
    }
}
