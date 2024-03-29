package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.config.rule.RuleComputer
import com.itangcent.intellij.constant.RuleConstant
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.PsiResolver
import com.itangcent.intellij.jvm.duck.DuckType

@Singleton
open class DefaultClassRuleConfig : ClassRuleConfig {

    @Inject
    protected val configReader: ConfigReader? = null

    @Inject
    protected val ruleComputer: RuleComputer? = null

    @Inject
    protected val jvmClassHelper: JvmClassHelper? = null

    @Inject
    protected val duckTypeHelper: DuckTypeHelper? = null

    @Inject
    protected val psiResolver: PsiResolver? = null

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

    override fun tryConvert(duckType: DuckType, context: PsiElement?): DuckType {
        if (context == null) return duckType
        return convertRule.safeComputeIfAbsent(duckType) {
            val convert = ruleComputer!!.computer(
                ClassRuleKeys.CLASS_CONVERT, duckType, context
            ) ?: return@safeComputeIfAbsent duckType
            return@safeComputeIfAbsent duckTypeHelper!!.resolve(convert, context) ?: duckType
        } as DuckType? ?: duckType
    }

    override fun tryConvert(psiClass: PsiClass): PsiClass {
        return convertRule.safeComputeIfAbsent(psiClass) {
            val convert = ruleComputer!!.computer(
                ClassRuleKeys.CLASS_CONVERT, psiClass, psiClass
            ) ?: return@safeComputeIfAbsent psiClass
            return@safeComputeIfAbsent psiResolver!!.findClass(convert, psiClass) ?: psiClass
        } as PsiClass? ?: psiClass
    }

    override fun maxDeep(): Int {
        return configReader?.first("max.deep")?.toIntOrNull() ?: RuleConstant.DEFAULT_MAX_DEEP
    }

    override fun maxElements(): Int {
        return configReader?.first("max.elements")?.toIntOrNull() ?: RuleConstant.DEFAULT_MAX_ELEMENTS
    }
}