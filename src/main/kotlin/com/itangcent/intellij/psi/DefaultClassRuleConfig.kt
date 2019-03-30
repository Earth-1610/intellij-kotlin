package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.intellij.psi.PsiField
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.config.SimpleRuleParse
import com.itangcent.intellij.config.SimpleStringRule
import java.util.*
import kotlin.collections.HashMap

class DefaultClassRuleConfig : ClassRuleConfig {
    @Inject
    private val simpleRuleParse: SimpleRuleParse? = null

    @Inject
    private val configReader: ConfigReader? = null

    override fun tryConvert(cls: String): String? {
        if (convertRule == null) convertRule = findConvertRule()
        return convertRule!![cls]
    }

    private var convertRule: Map<String, String>? = null

    private fun findConvertRule(): Map<String, String> {
        if (configReader == null) return Collections.emptyMap()

        val convertRule: HashMap<String, String> = HashMap()
        configReader.foreach({ key ->
            key.startsWith("json.rule.convert")
        }, { key, value ->
            val from = key.removePrefix("json.rule.convert")
                .removeSurrounding("[", "]")
            convertRule[from] = value
        })
        return convertRule
    }

    override fun findDoc(field: PsiField): String? {
        if (fieldDocReadRules == null) {
            fieldDocReadRules = findFieldDocReadRules()
        }

        return fieldDocReadRules!!
            .map { it(field, field, field) }
            .firstOrNull { !it.isNullOrBlank() }
    }

    private var fieldDocReadRules: List<SimpleStringRule>? = null

    private fun findFieldDocReadRules(): List<SimpleStringRule> {

        if (configReader == null) return Collections.emptyList()

        val fieldDocReadRules: ArrayList<SimpleStringRule> = ArrayList()

        configReader.foreach({ key ->
            key.startsWith("doc.resource")
        }, { _, value ->
            fieldDocReadRules.addAll(simpleRuleParse!!.parseStringRule(value))
        })

        return fieldDocReadRules
    }

    override fun getFieldName(field: PsiField): String? {

        if (fieldNameRules == null) {
            fieldNameRules = findFieldNameRules()
        }

        return fieldNameRules!!
            .map { it(field, field, field) }
            .firstOrNull { !it.isNullOrBlank() }
    }

    private var fieldNameRules: List<SimpleStringRule>? = null

    private fun findFieldNameRules(): List<SimpleStringRule> {

        if (configReader == null) return Collections.emptyList()

        val fieldNameRules: ArrayList<SimpleStringRule> = ArrayList()

        configReader.foreach({ key ->
            key.startsWith("json.rule.field.name")
        }, { _, value ->
            fieldNameRules.addAll(simpleRuleParse!!.parseStringRule(value))
        })

        return fieldNameRules
    }

}