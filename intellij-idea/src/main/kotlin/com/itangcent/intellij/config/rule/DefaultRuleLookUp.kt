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
    protected val ruleParser: RuleParser? = null

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
    override fun <T : Rule<*>> lookUp(key: String, ruleType: KClass<T>): List<T> {
        return ruleCaches.safeComputeIfAbsent(key) {
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
                if (key.startsWith(filteredKey)) {
                    val filterTxt = key.removePrefix(lookUpKey)
                        .removeSurrounding("[", "]")

                    if (filterTxt.startsWith("#regex:")) {
                        val regexBooleanRule = RegexBooleanRule.compile(filterTxt, value)

                        if (ruleType == StringRule::class) {
                            ruleParser!!.parseStringRule(value)?.let {
                                rules.add(regexBooleanRule.filterWith(it) as T)
                            }
                        } else if (ruleType == BooleanRule::class) {
                            ruleParser!!.parseBooleanRule(value)?.let {
                                rules.add(regexBooleanRule.filterWith(it) as T)
                            }
                        }
                        return@foreach
                    }

                    val filter = ruleParser!!.parseBooleanRule(filterTxt)
                    if (filter != null) {
                        if (ruleType == StringRule::class) {
                            ruleParser.parseStringRule(value)?.let {
                                rules.add(StringRule.filterWith(filter, it) as T)
                            }
                        } else if (ruleType == BooleanRule::class) {
                            ruleParser.parseBooleanRule(value)?.let {
                                rules.add(BooleanRule.filterWith(filter, it) as T)
                            }
                        }
                        return@foreach
                    }
                    logger!!.warn("error to parse $filterTxt")
                }


                if (ruleType == StringRule::class) {
                    ruleParser!!.parseStringRule(value)?.let {
                        rules.add(it as T)
                    }
                } else if (ruleType == BooleanRule::class) {
                    ruleParser!!.parseBooleanRule(value)?.let {
                        rules.add(it as T)
                    }
                }

            } catch (e: Exception) {
                logger!!.traceError("error to parse module rule:$key=$value", e)
            }
        }
        return rules.takeIf { it.isNotEmpty() } ?: emptyList()
    }

}