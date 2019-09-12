package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.intellij.config.ConfigReader
import java.util.*
import kotlin.collections.HashMap

@Singleton
open class DefaultClassRuleConfig : ClassRuleConfig {

    @Inject
    protected val configReader: ConfigReader? = null

    private var convertRule: Map<String, String>? = null

    override fun tryConvert(cls: String): String? {

        if (convertRule == null) {
            synchronized(this) {
                if (convertRule == null) {
                    convertRule = findConvertRule()
                }
            }
        }

        return convertRule!![cls]
    }

    fun findConvertRule(): Map<String, String> {
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
}