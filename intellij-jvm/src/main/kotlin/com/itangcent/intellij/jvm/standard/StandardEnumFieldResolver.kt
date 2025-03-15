package com.itangcent.intellij.jvm.standard

import com.google.inject.ImplementedBy
import com.intellij.psi.*
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.NumberUtils
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.jvm.PsiUtils
import com.itangcent.intellij.jvm.adapt.maybeGetterMethodPropertyName
import com.itangcent.intellij.jvm.adapt.propertyName
import com.itangcent.intellij.jvm.psi.PsiClassUtil
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
        var fields = if (construct == null) {
            resolveWithoutConstruct(psiEnumConstant)
        } else {
            resolveFieldsWithResolvedConstructor(psiEnumConstant, construct)
        }

        // Add getter method values to the fields
        fields = resolveGetterMethods(psiEnumConstant, fields ?: emptyMap())
        return fields.clean()
    }

    private fun Map<String, Any?>.clean(): Map<String, Any?>? {
        return filterKeys { !it.endsWith("()") }
    }

    private fun resolveFieldsWithResolvedConstructor(
        psiEnumConstant: PsiEnumConstant,
        construct: PsiMethod
    ): Map<String, Any?>? {
        val params = HashMap<String, Any?>()
        params.fillEnumMethods(psiEnumConstant)

        val expressions = psiEnumConstant.argumentList?.expressions
        val parameters = construct.parameterList.parameters
        if (expressions != null && parameters.isNotEmpty()) {
            if (parameters.last().isVarArgs) {
                for (i in 0 until parameters.size - 1) {
                    params[parameters[i].name] = PsiUtils.resolveExpr(expressions[i])
                }
                try {
                    //resolve varArgs
                    val lastVarArgParam: ArrayList<Any?> = ArrayList(1)
                    params[parameters[parameters.size - 1].name] = lastVarArgParam
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

    private fun HashMap<String, Any?>.fillEnumMethods(
        psiEnumConstant: PsiEnumConstant
    ) {
        this.computeIfAbsent("name()") { psiEnumConstant.name }
        this.computeIfAbsent("ordinal()") { psiEnumConstant.containingClass?.fields?.indexOf(psiEnumConstant) }
    }

    private val evaluatorCache = HashMap<PsiMethod, EnumFieldEvaluator>()

    private fun parseEvaluatorForResolvedConstruct(construct: PsiMethod): EnumFieldEvaluator {
        return evaluatorCache.computeIfAbsent(construct) {
            doParseEvaluatorForResolvedConstruct(it)
        }
    }

    private fun doParseEvaluatorForResolvedConstruct(construct: PsiMethod): EnumFieldEvaluator {
        val body = construct.body ?: return DirectCopyEnumFieldEvaluator
        var children = body.children
        val l = children.indexOfFirst { it.text == "{" }
        val r = children.indexOfLast { it.text == "}" }
        children = children.copyOfRange(l + 1, r)
        if (children.isEmpty()) {
            return DirectCopyEnumFieldEvaluator
        }
        try {
            return tryResolveConstructSimply(children)
        } catch (_: Exception) {
        }
        try {
            return tryResolveConstructAsScript(children)
        } catch (_: Exception) {
        }
        return DirectCopyEnumFieldEvaluator
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
        if (enumFieldEvaluators.all { it is IdentityFieldEvaluator }) {
            return DirectCopyEnumFieldEvaluator
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
                return IdentityFieldEvaluator(fieldName)
            }
            return SimpleAssignEnumFieldEvaluator(fieldName, rExpressionText)
        }
        return ConstantAssignEnumFieldEvaluator(fieldName, rExpressionResolveValue)
    }

    private fun tryResolveConstructAsScript(exps: Array<PsiElement>): EnumFieldEvaluator {
        val script = exps.filter { it !is PsiWhiteSpace }
            .joinToString(separator = "\n") { it.text }
        return GroovyEnumFieldEvaluator(script)
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
        val selectedConstructor = preferConstructor ?: return null
        val parameters = selectedConstructor.parameterList.parameters
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
            val enumFieldEvaluators = parseEvaluatorForUnresolvedConstruct(selectedConstructor)
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
            return GroovyEnumFieldEvaluator(script)
        } catch (e: Exception) {
            LOG.traceWarn("failed resolve construct: ${construct.text}", e)
        }
        return DirectCopyEnumFieldEvaluator
    }

    /**
     * Resolves getter methods of the enum constant and adds their values to the fields map
     */
    private fun resolveGetterMethods(psiEnumConstant: PsiEnumConstant, fields: Map<String, Any?>): Map<String, Any?> {
        val enumClass = psiEnumConstant.containingClass ?: return fields

        val result = HashMap<String, Any?>(fields)

        val allMethods = PsiClassUtil.getAllMethods(enumClass)

        // Create a map to store method evaluators
        val methodEvaluators = HashMap<String, EnumFieldEvaluator>()

        allMethods
            .filter { method ->
                method.name.maybeGetterMethodPropertyName()
                        && method.parameterList.parametersCount == 0
                        && method.returnType != null
                        // Filter out methods without implementations (abstract methods and interface methods)
                        && method.body != null
            }
            .forEach { method ->
                try {
                    // Determine property name based on method name
                    val propertyName = method.name.propertyName()
                    if (methodEvaluators.containsKey(propertyName)) {
                        return@forEach
                    }
                    evaluatorCache.safeComputeIfAbsent(method) {
                        createEvaluatorForGetter(method, propertyName)
                    }?.let { methodEvaluators[propertyName] = it }
                } catch (e: Exception) {
                    LOG.traceWarn("Failed to resolve getter method: ${method.name} for enum ${psiEnumConstant.name}", e)
                }
            }

        // Apply all evaluators to the result map
        if (methodEvaluators.isNotEmpty()) {
            val params = HashMap<String, Any?>()

            // Add existing fields to params so they can be referenced
            params.putAll(fields)
            params.fillEnumMethods(psiEnumConstant)

            methodEvaluators.forEach { (propertyName, evaluator) ->
                try {
                    val value = evaluator.evaluate(params, result)
                    if (value != null && value != Unit) {
                        result[propertyName] = value
                    }
                } catch (e: Exception) {
                    LOG.traceWarn("Failed to evaluate getter for $propertyName in ${psiEnumConstant.name}", e)
                }
            }
        }

        return result
    }

    /**
     * Creates an EnumFieldEvaluator for a getter method
     */
    private fun createEvaluatorForGetter(
        method: PsiMethod,
        propertyName: String
    ): EnumFieldEvaluator? {
        // Methods without body are already filtered out in resolveGetterMethods
        val bodyText = method.body?.text?.trim() ?: return null

        // Methods that return a field directly - match only if it's a simple field return
        // with no operators or additional expressions
        val returnFieldMatch =
            Regex("^\\s*return\\s+(?:this\\.)?(\\w+)\\s*;?\\s*$", RegexOption.MULTILINE).find(bodyText)

        // Only use SimpleAssignEnumFieldEvaluator for simple field returns
        if (returnFieldMatch != null) {
            val fieldName = returnFieldMatch.groupValues[1]
            return SimpleAssignEnumFieldEvaluator(propertyName, fieldName)
        }

        // For more complex methods, try to use a script evaluator
        try {
            val script = bodyText
                .substringAfter('{')
                .substringBeforeLast('}')
            return GroovyEnumFieldEvaluator(script)
        } catch (e: Exception) {
            LOG.traceWarn("Failed to create script evaluator for ${method.name}", e)
        }

        return null
    }
}

interface EnumFieldEvaluator {
    fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>): Any?
}

object DirectCopyEnumFieldEvaluator : EnumFieldEvaluator {
    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>) {
        fields.putAll(params)
    }
}

class IdentityFieldEvaluator(private val fieldName: String) : EnumFieldEvaluator {
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

    // Modify the script to replace direct enum method calls with calls through the wrapper
    private val preprocessedScript: String by lazy { preprocessScript(script) }

    override fun evaluate(params: HashMap<String, Any?>, fields: HashMap<String, Any?>): Any? {
        val engineBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)

        // Create a wrapper object for enum methods to avoid conflicts with parameter names
        val enumMethods = createEnumMethodsWrapper(params)
        engineBindings["enumMethods"] = enumMethods

        // Add all parameters to bindings
        engineBindings.putAll(params)
        engineBindings["it"] = fields

        val res = scriptEngine.eval(preprocessedScript, engineBindings)
        if (fields.isNotEmpty()) {
            val keys = ArrayList(fields.keys)
            for (key in keys) {
                val value = fields[key]
                if (value is Double) {
                    fields[key] = fixDouble(value)
                } else if (value is Float) {
                    fields[key] = fixFloat(value)
                }
            }
        } else {
            fields.putAll(params)
        }
        return res
    }

    /**
     * Creates a wrapper object that contains enum methods
     */
    private fun createEnumMethodsWrapper(params: HashMap<String, Any?>): Any {
        // Create a map to hold the enum methods
        val methodMap = HashMap<String, () -> Any?>()

        // Add name() method if available
        if (params.containsKey("name()")) {
            methodMap["name"] = { params["name()"] }
        }

        // Add ordinal() method if available
        if (params.containsKey("ordinal()")) {
            methodMap["ordinal"] = { params["ordinal()"] }
        }

        // Create and return a dynamic object with these methods
        return object {
            fun name(): Any? = methodMap["name"]?.invoke()
            fun ordinal(): Any? = methodMap["ordinal"]?.invoke()
        }
    }

    /**
     * Preprocesses the script to replace direct enum method calls with calls through the wrapper
     */
    private fun preprocessScript(script: String): String {
        // Replace standalone name() and ordinal() calls with enumMethods.name() and enumMethods.ordinal()
        // but be careful not to replace method calls on other objects
        var modifiedScript = script

        // First, handle this.name() and this.ordinal() calls
        val thisEnumMethodPattern = Regex("this\\.(name|ordinal)\\(\\)")
        modifiedScript = thisEnumMethodPattern.replace(modifiedScript) {
            "enumMethods.${it.groupValues[1]}()"
        }

        // Then handle standalone name() and ordinal() calls not preceded by a dot or word character
        val enumMethodPattern = Regex("(?<![.\\w])\\b(name|ordinal)\\(\\)")
        modifiedScript = enumMethodPattern.replace(modifiedScript) {
            "enumMethods.${it.groupValues[1]}()"
        }

        // Finally replace any remaining this. references with it.
        return modifiedScript.replace("this.", "it.")
    }

    private fun fixFloat(value: Float): Float =
        NumberUtils.fixFloat(value)

    private fun fixDouble(value: Double): Double =
        NumberUtils.fixDouble(value)

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
