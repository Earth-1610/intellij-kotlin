package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.common.logger.traceError
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.psi.ContextSwitchListener
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Singleton
open class DefaultRuleLookUp : RuleLookUp {

    @Inject
    protected val logger: Logger? = null

    @Inject
    protected val configReader: ConfigReader? = null

    @Inject
    protected lateinit var ruleParser: RuleParser

    @Inject
    protected val contextSwitchListener: ContextSwitchListener? = null

    protected var ruleCaches: ConcurrentHashMap<String, List<Rule<*>>> =
        ConcurrentHashMap(10)

    @PostConstruct
    fun init() {
        contextSwitchListener!!.onModuleChange {
            ruleCaches.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> lookUp(key: String, ruleType: KClass<T>): List<Rule<T>> {
        return ruleCaches.safeComputeIfAbsent(key) {
            doLookUp(key, ruleType)
        } as List<Rule<T>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> doLookUp(lookUpKey: String, ruleType: KClass<T>): List<Rule<T>> {
        val rules: ArrayList<Any> = ArrayList()
        val filteredKey = "$lookUpKey["
        configReader!!.foreach({ key ->
            key == lookUpKey
                    || key.startsWith(filteredKey)
        }) { key, value ->
            try {
                if (key.startsWith(filteredKey)) {
                    val filterTxt = key.removePrefix(lookUpKey)
                        .removeSurrounding("[", "]")

                    if (filterTxt.startsWith("#regex:")) {
                        val regexBooleanRule = RegexBooleanRule.compile(filterTxt, value)
                        ruleParser.parseRule(value, ruleType)?.let {
                            if (it is StringRule) {
                                rules.add(regexBooleanRule.asFilterOf(it))
                            } else {
                                rules.add(it.filterWith(regexBooleanRule))
                            }
                        }
                        return@foreach
                    }

                    val filter = ruleParser.parseBooleanRule(filterTxt)
                    if (filter != null) {
                        ruleParser.parseRule(value, ruleType)?.let {
                            rules.add(it.filterWith(filter))
                        }
                        return@foreach
                    }
                    logger!!.warn("error to parse $filterTxt")
                }

                ruleParser.parseRule(value, ruleType)?.let {
                    rules.add(it)
                }
            } catch (e: Exception) {
                logger!!.traceError("error to parse module rule:$key=$value", e)
            }
        }
        return (rules.takeIf { it.isNotEmpty() } ?: emptyList()) as List<Rule<T>>
    }

}