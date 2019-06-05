package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.PsiField
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.config.SimpleRuleParse
import com.itangcent.intellij.config.SimpleStringRule
import com.itangcent.intellij.config.context.PsiFieldContext
import com.itangcent.intellij.util.reduceSafely

abstract class AbstractClassRuleConfig : ClassRuleConfig {
    @Inject
    protected val simpleRuleParse: SimpleRuleParse? = null
    @Inject
    protected val configReader: ConfigReader? = null
    private var convertRule: Map<String, String>? = null
    private var fieldDocReadRules: List<SimpleStringRule>? = null
    private var fieldNameRules: List<SimpleStringRule>? = null

    override fun tryConvert(cls: String): String? {
        if (convertRule == null) convertRule = findConvertRule()
        return convertRule!![cls]
    }

    protected abstract fun findConvertRule(): Map<String, String>

    override fun findDoc(field: PsiField): String? {
        if (fieldDocReadRules == null) {
            fieldDocReadRules = findFieldDocReadRules()
        }
        val context = PsiFieldContext(field)

        return fieldDocReadRules!!
            .map { it(context) }
            .filter { !it.isNullOrBlank() }
            .reduceSafely { s1, s2 -> s1 + '\n' + s2 }
    }

    protected abstract fun findFieldDocReadRules(): List<SimpleStringRule>

    override fun getFieldName(field: PsiField): String? {

        if (fieldNameRules == null) {
            fieldNameRules = findFieldNameRules()
        }
        val context = PsiFieldContext(field)

        return fieldNameRules!!
            .map { it(context) }
            .firstOrNull { !it.isNullOrBlank() }
    }

    protected abstract fun findFieldNameRules(): List<SimpleStringRule>
}