package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.config.SimpleBooleanRule
import com.itangcent.intellij.config.SimpleRuleParser
import com.itangcent.intellij.config.SimpleStringRule
import java.util.*
import kotlin.collections.HashMap

open class DefaultClassRuleConfig : AbstractClassRuleConfig() {

    @Inject
    protected val simpleRuleParse: SimpleRuleParser? = null

    @Inject
    protected val configReader: ConfigReader? = null

    override fun findConvertRule(): Map<String, String> {
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

    override fun findFieldDocReadRules(): List<SimpleStringRule> {

        if (configReader == null) return Collections.emptyList()

        val fieldDocReadRules: ArrayList<SimpleStringRule> = ArrayList()

        configReader.foreach({ key ->
            key.startsWith("doc.field")
        }, { _, value ->
            fieldDocReadRules.addAll(simpleRuleParse!!.parseStringRule(value))
        })

        return fieldDocReadRules
    }

    override fun findFieldNameRules(): List<SimpleStringRule> {

        if (configReader == null) return Collections.emptyList()

        val fieldNameRules: ArrayList<SimpleStringRule> = ArrayList()

        configReader.foreach({ key ->
            key.startsWith("json.rule.field.name")
        }, { _, value ->
            fieldNameRules.addAll(simpleRuleParse!!.parseStringRule(value))
        })

        return fieldNameRules
    }

    override fun findFieldIgnoreRules(): List<SimpleBooleanRule>? {

        if (configReader == null) return Collections.emptyList()

        val fieldIgnoreRules: ArrayList<SimpleBooleanRule> = ArrayList()

        configReader.foreach({ key ->
            key.startsWith("json.rule.field.ignore")
        }, { _, value ->
            fieldIgnoreRules.addAll(simpleRuleParse!!.parseBooleanRule(value))
        })

        return fieldIgnoreRules
    }

}