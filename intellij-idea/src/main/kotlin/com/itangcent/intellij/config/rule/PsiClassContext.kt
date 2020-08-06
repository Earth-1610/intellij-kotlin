package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.SimpleExtensible
import com.itangcent.common.utils.cache
import com.itangcent.intellij.jvm.element.ExplicitClass

open class PsiClassContext : SimpleExtensible, RuleContext {

    protected var psiClass: PsiClass

    constructor(psiClass: PsiClass) {
        this.psiClass = psiClass
    }

    override fun getResource(): PsiElement {
        return psiClass
    }

    override fun getName(): String? {
        return this.cache("_name") { psiClass.qualifiedName }
    }

    override fun getSimpleName(): String? {
        return this.cache("_simpleName") { psiClass.name }
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiClass
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiClass
    }

    override fun toString(): String {
        return getName() ?: "anonymous"
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