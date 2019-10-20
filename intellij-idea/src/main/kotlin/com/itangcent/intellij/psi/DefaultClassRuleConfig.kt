package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.JvmClassHelper
import java.util.*

@Singleton
open class DefaultClassRuleConfig : ClassRuleConfig {
    @Inject
    protected val ruleComputer: RuleComputer? = null
    @Inject
    protected val jvmClassHelper: JvmClassHelper? = null
    @Inject
    protected val duckTypeHelper: DuckTypeHelper? = null

    private var convertRule: HashMap<Any, Any> = LinkedHashMap()

    override fun tryConvert(psiType: PsiType, context: PsiElement?): PsiType {
        return convertRule.safeComputeIfAbsent(psiType) {
            val realContext =
                context ?: jvmClassHelper!!.resolveClassInType(psiType) ?: return@safeComputeIfAbsent psiType
            val convert = ruleComputer!!.computer(
                ClassRuleKeys.CLASS_CONVERT, psiType, realContext
            ) ?: return@safeComputeIfAbsent psiType
            return@safeComputeIfAbsent duckTypeHelper!!.findType(convert, realContext) ?: psiType
        } as PsiType? ?: psiType
    }

    override fun tryConvert(psiClass: PsiClass): PsiClass {
        return convertRule.safeComputeIfAbsent(psiClass) {
            val convert = ruleComputer!!.computer(
                ClassRuleKeys.CLASS_CONVERT, psiClass, psiClass
            ) ?: return@safeComputeIfAbsent psiClass
            return@safeComputeIfAbsent duckTypeHelper!!.findClass(convert, psiClass) ?: psiClass
        } as PsiClass? ?: psiClass
    }

}