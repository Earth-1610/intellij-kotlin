package com.itangcent.intellij.config.rule

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.itangcent.common.utils.SimpleExtensible

open class StringRuleContext : SimpleExtensible, RuleContext {

    private val str: String
    private var context: PsiElement

    constructor(str: String, context: PsiElement) {
        this.context = context
        this.str = str
    }

    override fun getResource(): PsiElement {
        return context
    }

    override fun getName(): String? {
        return str
    }

    override fun asPsiDocCommentOwner(): PsiDocCommentOwner? {
        return null
    }

    override fun asPsiModifierListOwner(): PsiModifierListOwner? {
        return null
    }

    override fun toString(): String {
        return this.str
    }

}
