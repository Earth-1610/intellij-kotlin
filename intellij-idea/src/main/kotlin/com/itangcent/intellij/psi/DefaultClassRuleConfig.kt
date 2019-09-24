package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.logger.Logger
import java.util.*
import java.util.regex.Pattern

@Singleton
open class DefaultClassRuleConfig : AbstractClassRuleConfig() {

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val configReader: ConfigReader? = null

    private var convertRule: List<(String) -> String?>? = null

    override fun convertByRule(cls: String): String {

        if (convertRule == null) {
            synchronized(this) {
                if (convertRule == null) {
                    convertRule = findConvertRule()
                }
            }
        }

        for (rule in this.convertRule!!) {
            val convert = rule(cls)
            if (convert.isNullOrBlank()) {
                continue
            }
            return convert
        }

        return cls
    }

    private fun findConvertRule(): List<(String) -> String?>? {
        if (configReader == null) return Collections.emptyList()

        val convertRule: LinkedList<(String) -> String?> = LinkedList()
        configReader.foreach({ key ->
            key.startsWith("json.rule.convert")
        }, { key, value ->
            val from = key.removePrefix("json.rule.convert")
                .removeSurrounding("[", "]")
            if (!from.isNullOrBlank()) {
                try {
                    parseRule(from, value)?.let { convertRule.add(it) }
                } catch (e: Throwable) {
                    logger!!.error("error to parse rule:$key=$value")
                }
            }
        })
        return convertRule
    }

    private fun parseRule(key: String, value: String): ((String) -> String?)? {
        if (key.startsWith("\$regex:")) {
            val keyRegexStr = key.removePrefix("\$regex:")
            val matcherValue = Pattern.compile("\\$\\{(\\d+)}").matcher(value)
            val valueGroups: LinkedList<Int> = LinkedList()
            while (matcherValue.find()) {
                valueGroups.add(matcherValue.group(1).toInt())
            }
            if (valueGroups.isEmpty()) {
                return { cls ->
                    val matcher = Pattern.compile(keyRegexStr).matcher(cls)
                    when {
                        matcher.matches() -> value
                        else -> null
                    }
                }
            } else {
                return { cls ->
                    val matcher = Pattern.compile(keyRegexStr).matcher(cls)
                    if (matcher.matches()) {
                        var ret = value
                        for (valueGroup in valueGroups) {
                            ret = ret.replace("\${$valueGroup}", matcher.group(valueGroup))
                        }
                        ret
                    } else {
                        null
                    }
                }
            }
        } else {
            return { cls ->
                when (cls) {
                    key -> value
                    else -> null
                }
            }
        }
    }

}