package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.SimpleExtensible
import com.itangcent.common.utils.cache
import com.itangcent.intellij.jvm.element.ExplicitMethod
import com.itangcent.intellij.psi.PsiClassUtils

open class PsiMethodContext : SimpleExtensible, RuleContext {

    protected var psiMethod: PsiMethod

    constructor(psiMethod: PsiMethod) {
        this.psiMethod = psiMethod
    }

    override fun getResource(): PsiElement {
        return psiMethod
    }

    override fun getName(): String? {
        return this.cache("_name") { PsiClassUtils.qualifiedNameOfMethod(psiMethod) }
    }

    override fun getSimpleName(): String? {
        return this.cache("_simpleName") { psiMethod.name }
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiMethod
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return psiMethod
    }

    override fun toString(): String {
        return getName() ?: "anonymous"
    }
}

class ExplicitMethodContext : PsiMethodContext {
    private var explicitMethod: ExplicitMethod

    constructor(explicitMethod: ExplicitMethod) : super(explicitMethod.psi()) {
        this.explicitMethod = explicitMethod
    }

    override fun getCore(): Any? {
        return explicitMethod
    }
}