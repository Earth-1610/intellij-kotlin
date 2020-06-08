package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.SimpleExtensible
import com.itangcent.intellij.jvm.element.ExplicitField

open class PsiFieldContext : SimpleExtensible, RuleContext {

    protected var psiField: PsiField

    constructor(psiField: PsiField) {
        this.psiField = psiField
    }

    override fun getResource(): PsiElement {
        return psiField
    }

    override fun getName(): String? {
        return psiField.name
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner {
        return psiField
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner {
        return psiField
    }
}

class ExplicitFieldContext : PsiFieldContext {
    private var explicitField: ExplicitField

    constructor(explicitField: ExplicitField) : super(explicitField.psi()) {
        this.explicitField = explicitField
    }

    override fun getCore(): Any? {
        return explicitField
    }
}