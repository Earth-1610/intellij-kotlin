package com.itangcent.intellij.jvm.standard

import com.google.inject.ImplementedBy
import com.intellij.psi.*
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.GsonUtils
import com.itangcent.intellij.jvm.PsiUtils
import java.util.*
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * resolve enum field
 *
 * @author tangcent
 */
@ImplementedBy(StandardEnumFieldResolverImpl::class)
interface StandardEnumFieldResolver {

    fun resolveEnumFields(psiEnumConstant: PsiEnumConstant): Map<String, Any?>?
}

class StandardEnumFieldResolverImpl : StandardEnumFieldResolver {

    companion object : Log()

    override fun resolveEnumFields(psiEnumConstant: PsiEnumConstant): Map<String, Any?>? {
        val construct = psiEnumConstant.resolveConstructor()
        return if (construct == null) {
            resolveWithoutConstruct(psiEnumConstant)
        } else {
            resolveFieldsWithResolvedConstructor(psiEnumConstant, construct)
        }
    }

    private fun resolveFieldsWithResolvedConstructor(
        psiEnumConstant: PsiEnumConstant,
        construct: PsiMethod
    ): Map<String, Any?>? {
        val params = HashMap<String, Any?>()
        val expressions = psiEnumConstant.argumentList?.expressions
        val parameters = construct.parameterList.parameters
        if (expressions != null && parameters.isNotEmpty()) {
            if (parameters.last().isVarArgs) {
                for (i in 0 until parameters.size - 1) {
                    params[parameters[i].name!!] = PsiUtils.resolveExpr(expressions[i])
                }
                try {
                    //resolve varArgs
                    val lastVarArgParam: ArrayList<Any?> = ArrayList(1)
                    params[parameters[parameters.size - 1].name!!] = lastVarArgParam
                    for (i in parameters.size - 1..expressions.size) {
                        lastVarArgParam.add(PsiUtils.resolveExpr(expressions[i]))
                    }
                } catch (_: Throwable) {
                }
            } else {
                for ((i, parameter) in parameters.withIndex()) {
                    try {
                        params[parameter.name] = PsiUtils.resolveExpr(expressions[i])
                    } catch (_: Throwable) {
                    }
                }
            }
        }
        return try {
            val enumFieldEvaluators = parseEvaluatorForResolvedConstruct(construct)
            val fields = HashMap<String, Any?>()
            enumFieldEvaluators.evaluate(params, fields)
            fields
        } catch (e: Exception) {
            LOG.traceWarn("failed resolve enum ${psiEnumConstant.containingClass?.text}", e)
            params
        }
    }

    private val evaluatorCache = HashMap<PsiMethod, EnumFieldEvaluator>()

    private fun parseEvaluatorForResolvedConstruct(construct: PsiMethod): EnumFieldEvaluator {
        return evaluatorCache.computeIfAbsent(construct) {
            doParseEvaluatorForResolvedConstruct(it)
        }
    }

    private fun doParseEvaluatorForResolvedConstruct(construct: PsiMethod): EnumFieldEvaluator {
        val body = construct.body ?: return NOPEnumFieldEvaluator
        var children = body.children
        val l = children.indexOfFirst { it.text == "{" }
        val r = children.indexOfLast { it.text == "}" }
        children = children.copyOfRange(l + 1, r)
        if (children.isEmpty()) {
            return NOPEnumFieldEvaluator
        }
        try {
            return tryResolveConstructSimply(children)
        } catch (e: Exception) {
        }
        try {
            return tryResolveConstructAsScript(children)
        } catch (e: Exception) {
        }
        return NOPEnumFieldEvaluator
    }

    private fun tryResolveConstructSimply(exps: Array<PsiElement>): EnumFieldEvaluator {
        val enumFieldEvaluators = LinkedList<EnumFieldEvaluator>()
        for (exp in exps) {
            if (exp is PsiWhiteSpace) {
                continue
            }
            if (exp !is PsiExpressionStatement) {
                throw RuntimeException("failed")
            }
            enumFieldEvaluators.add(resolvePsiExpressionStatement(exp))
        }
        if (enumFieldEvaluators.isEmpty()) {
            throw RuntimeException("failed")
        }
        if (enumFieldEvaluators.all { it is MinimalEnumFieldEvaluator }) {
            return NOPEnumFieldEvaluator
        }
        return CompositedEnumFieldEvaluator(enumFieldEvaluators)
    }

    private fun resolvePsiExpressionStatement(psiExpressionStatement: PsiExpressionStatement): EnumFieldEvaluator {
        return psiExpressionStatement.children.first {
            it is PsiAssignmentExpression
        }.let { resolvePsiAssignmentExpression(it as PsiAssignmentExpression) }
    }

    private fun resolvePsiAssignmentExpression(psiAssignmentExpression: PsiAssignmentExpression): EnumFieldEvaluator {
        val rExpression = psiAssignmentExpression.rExpression!!
        if (rExpression !is PsiReferenceExpression) {
            throw RuntimeException("failed")
        }
        val lExpression = psiAssignmentExpression.lExpression
        val fieldName = lExpression.text.removePrefix("this.")
        val rExpressionText = rExpression.text
        val rExpressionResolveValue = PsiUtils.resolveExpr(rExpression)
        if (rExpressionResolveValue == null || rExpressionResolveValue == rExpressionText) {
            if (rExpressionText == fieldName) {
                return MinimalEnumFieldEvaluator(fieldName)
            }
            return SimpleAssignEnumFieldEvaluator(fieldName, rExpressionText)
        }
        return ConstantAssignEnumFieldEvaluator(fieldName, rExpressionResolveValue)
    }

    private fun tryResolveConstructAsScript(exps: Array<PsiElement>): EnumFieldEvaluator {
        val script = exps.filter { it !is PsiWhiteSpace }
            .joinToString(separator = "\n") { it.text }
        return GroovyEnumFieldEvaluator(script.replace("this.", "it."))
    }

    private fun resolveWithoutConstruct(
        psiEnumConstant: PsiEnumConstant
    ): Map<String, Any?>? {
        var constantText = psiEnumConstant.text ?: ""
        val paramArray: Array<Any?>

        if (!constantText.contains('(')) {
            //Enum field with NoArgConstructor
            paramArray = emptyArray()
        } else {
            constantText = constantText.substringAfter('(')
                .substringBeforeLast(')')
            paramArray = if (constantText.isBlank()) {
                emptyArray()
            } else {
                try {
                    GsonUtils.fromJson("[$constantText]", Array<Any?>::class.java)
                } catch (e: Exception) {
                    LOG.traceWarn("failed resolve {$constantText} in ${psiEnumConstant.containingClass?.name}", e)
                    return null
                }
            }
        }

        val constructors = psiEnumConstant.containingClass?.constructors
        if (constructors.isNullOrEmpty()) {
            if (paramArray.isEmpty()) {
                return emptyMap()
            }
            LOG.warn("no constructor of ${psiEnumConstant.containingClass?.name} be found")
            return null
        }

        var preferConstructor: PsiMethod? = null
        if (constructors.size > 1) {
            constructors.forEach {
                if (it.parameterList.parameters.size == paramArray.size) {
                    if (preferConstructor == null) {
                        preferConstructor = it
                    } else {
                        LOG.warn("can not select constructor of ${psiEnumConstant.containingClass?.name}")
                        return null
                    }
                }
            }
            if (preferConstructor == null) {
                LOG.warn("can not select constructor of ${psiEnumConstant.containingClass?.name}")
                return null
            }
        } else {
            preferConstructor = constructors[0]
        }

        val params = HashMap<String, Any?>()
        val parameters = preferConstructor!!.parameterList.parameters
        if (parameters.isNotEmpty()) {
            if (parameters.last().isVarArgs) {
                for (i in 0 until parameters.size - 1) {
                    params[parameters[i].name] = paramArray[i]
                }
                try {
                    //resolve varArgs
                    val lastVarArgParam: ArrayList<Any?> = ArrayList(1)
                    params[parameters[parameters.size - 1].name] = lastVarArgParam
                    for (i in parameters.size - 1..paramArray.size) {
                        lastVarArgParam.add(paramArray[i])
                    }
                } catch (_: Throwable) {
                }
            } else {
                for ((i, parameter) in parameters.withIndex()) {
                    try {
                        params[parameter.name] = paramArray[i]
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        return try {
            val enumFieldEvaluators = parseEvaluatorForUnresolvedConstruct(preferConstructor!!)
            val fields = HashMap<String, Any?>()
            enumFieldEvaluators.evaluate(params, fields)
            fields
        } catch (e: Exception) {
            LOG.traceWarn("failed resolve enum ${psiEnumConstant.containingClass?.text}", e)
            params
        }
    }


    private fun parseEvaluatorForUnresolvedConstruct(construct: PsiMethod): EnumFieldEvaluator {
        return evaluatorCache.computeIfAbsent(construct) {
            doParseEvaluatorForUnresolvedConstruct(it)
        }
    }

    private fun doParseEvaluatorForUnresolvedConstruct(construct: PsiMethod): EnumFieldEvaluator {
        try {
            val script = construct.text
                .substringAfter('{')
                .substringBeforeLast('}')
                .replace("this.", "it.")
            return GroovyEnumFieldEvaluator(script)
        } catch (e: Exception) {
            LOG.traceWarn("failed resolve construct: ${construct.text}", e)
        }
        return NOPEnumFieldEvaluator
    }
}

interface EnumFieldEvaluator {
    fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>)
}

object NOPEnumFieldEvaluator : EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        fields.putAll(params)
    }
}

class MinimalEnumFieldEvaluator(private val fieldName: String) : EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        fields[fieldName] = params[fieldName]
    }
}

class SimpleAssignEnumFieldEvaluator(
    private val fieldName: String,
    private val paramName: String
) : EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        fields[fieldName] = params[paramName]
    }
}

class ConstantAssignEnumFieldEvaluator(
    private val fieldName: String,
    private val value: Any?
) : EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        fields[fieldName] = value
    }
}

abstract class ScriptEnumFieldEvaluator(
    private val script: String
) : EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        val engineBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)
        engineBindings.putAll(params)
        engineBindings["it"] = fields
        scriptEngine.eval(script, engineBindings)
        if (fields.isNotEmpty()) {
            val keys = ArrayList(fields.keys)
            for (key in keys) {
                val value = fields[key]
                if (value is Double) {
                    fields[key] = fixDouble(fields[key] as Double)
                } else if (value is Float) {
                    fields[key] = fixDouble((fields[key] as Float).toDouble()).toFloat()
                }
            }
        } else {
            fields.putAll(params)
        }
    }

    private fun fixDouble(value: Double): Double {
        try {//fix double
            val str = (value - (value.toLong())).toString()
            if (str.contains("0000")) {
                return (value.toLong().toDouble()) +
                        str.substringBefore("0000").removeSuffix(".").toDouble()
            }
            if (str.contains("9999")) {
                val before = str.substringBefore("9999").removeSuffix(".")//0.0
                if (before.length < 3) {
                    return value.toLong().toDouble() + 1.0
                }
                return value.toLong().toDouble() + before.toDouble() +
                        ("0." + "0".repeat(before.length - 3) + "1").toDouble()
            }
        } catch (e: Exception) {
            LOG.traceWarn("failed fixDouble $value", e)
        }
        return value
    }

    private val scriptEngine: ScriptEngine by lazy {
        buildScriptEngine()
    }

    private fun buildScriptEngine(): ScriptEngine {
        val manager = ScriptEngineManager()
        return manager.getEngineByName(scriptType())
    }

    abstract fun scriptType(): String

    companion object : Log()
}

class GroovyEnumFieldEvaluator(script: String) : ScriptEnumFieldEvaluator(script) {
    override fun scriptType(): String {
        return "groovy"
    }
}

class CompositedEnumFieldEvaluator(private val enumFieldEvaluators: Collection<EnumFieldEvaluator>) :
    EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        enumFieldEvaluators.forEach { it.evaluate(params, fields) }
    }
}
