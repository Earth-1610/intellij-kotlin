package com.itangcent.intellij.config

import com.google.inject.Inject
import com.intellij.psi.PsiField
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.jvm.ExtendProvider
import com.itangcent.intellij.psi.ClassRuleKeys

class RuleExtendProvider : ExtendProvider {

    @Inject
    private val ruleComputer: RuleComputer? = null

    override fun extraDoc(field: PsiField): String? {
        return ruleComputer?.computer(ClassRuleKeys.FIELD_DOC, field)
    }
}