package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.traceError
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Singleton
open class DefaultRuleLookUp : RuleLookUp {

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val configReader: ConfigReader? = null

    @Inject
    protected val ruleParser: RuleParser? = null

    protected var ruleCaches: ConcurrentHashMap<String, List<Rule<*>>> =
        ConcurrentHashMap(10)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Rule<*>> lookUp(key: String, ruleType: KClass<T>): List<T> {
        return ruleCaches.computeIfAbsent(key) {
            doLookUp(key, ruleType)
        } as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Rule<*>> doLookUp(lookUpKey: String, ruleType: KClass<T>): List<T> {
        val rules: ArrayList<T> = ArrayList()
        val filteredKey = "$lookUpKey["
        configReader!!.foreach({ key ->
            key == lookUpKey
                    || key.startsWith(filteredKey)
        }) { key, value ->
            try {
                if (key == lookUpKey) {
                    if (ruleType == StringRule::class) {
                        ruleParser!!.parseStringRule(value).forEach {
                            rules.add(it as T)
                        }
                    } else if (ruleType == BooleanRule::class) {
                        ruleParser!!.parseBooleanRule(value).forEach {
                            rules.add(it as T)
                        }
                    }
                } else if (key.startsWith(filteredKey)) {
                    val filterTxt = key.removePrefix(lookUpKey)
                        .removeSurrounding("[", "]")
                    val filter = ruleParser!!.parseBooleanRule(filterTxt)
                    if (ruleType == StringRule::class) {
                        ruleParser.parseStringRule(value).forEach {
                            rules.add(StringRule.filterWith(filter.union(), it) as T)
                        }
                    } else if (ruleType == BooleanRule::class) {
                        ruleParser.parseBooleanRule(value).forEach {
                            rules.add(BooleanRule.filterWith(filter.union(), it) as T)
                        }
                    }
                }
            } catch (e: Exception) {
                logger!!.error("error to parse module rule:$key=$value")
                logger.traceError(e)
            }
        }
        return when {
            rules.isEmpty() -> emptyList()
            else -> rules
        }
    }

}