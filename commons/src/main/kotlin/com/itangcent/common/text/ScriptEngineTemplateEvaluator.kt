package com.itangcent.common.text

import com.itangcent.common.utils.GsonUtils
import java.util.regex.Pattern
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext

abstract class ScriptEngineTemplateEvaluator : TemplateEvaluator {

    protected val scriptEngine: ScriptEngine by lazy {
        buildScriptEngine().also { initScripEngine(it) }
    }

    private fun buildScriptEngine(): ScriptEngine {
        val manager = ScriptEngineManager()
        return manager.getEngineByName(scriptType())
    }

    abstract fun scriptType(): String

    open fun initScripEngine(scriptEngine: ScriptEngine) {
    }

    override fun eval(str: String): Any? {
        return try {
            scriptEngine.eval(str)?.toString()
        } catch (e: Exception) {
            null
        }
    }
}

abstract class ScriptEngineTemplateEvaluatorWithSuperTemplateEvaluator(private val templateEvaluator: TemplateEvaluator) :
    ScriptEngineTemplateEvaluator() {

    private val scriptContext: ScriptContext by lazy {
        TemplateEvaluatorScriptContext(templateEvaluator)
    }

    override fun eval(str: String): Any? {
        return try {
            scriptEngine.eval(str, scriptContext)?.toString()
        } catch (e: Exception) {
            scriptContext.getAttribute("a")
            null
        }
    }
}

private class TemplateEvaluatorScriptContext(private val templateEvaluator: TemplateEvaluator) : SimpleScriptContext() {
    override fun getAttribute(name: String?): Any? {
        super.getAttribute(name)?.let { return it }
        return name?.takeIf { !it.startsWith("#jsr223") }?.let { templateEvaluator.eval(it)?.resolveNumeric() }
    }

    override fun getAttribute(name: String?, scope: Int): Any? {
        super.getAttribute(name, scope)?.let { return it }
        return name?.takeIf { !it.startsWith("#jsr223") }?.let { templateEvaluator.eval(it)?.resolveNumeric() }
    }

    override fun getAttributesScope(name: String?): Int {
        val scope = super.getAttributesScope(name)
        if (scope != -1 || name == null || name.startsWith("#jsr223")) {
            return scope
        }
        if (templateEvaluator.eval(name) != null) {
            return GLOBAL_SCOPE
        }
        return scope
    }

    private fun Any.resolveNumeric(): Any {
        if (this is String) {
            try {
                if (NUMERIC_PATTERN.matcher(this).matches()) {
                    return if (this.contains('.')) {
                        this.toDouble()
                    } else {
                        this.toLong()
                    }
                } else if (TRUE_OR_FALSE.contains(this)) {
                    return this.toBoolean()
                } else if (this.startsWith("{") && this.endsWith("}")) {
                    return GsonUtils.fromJson(this, Map::class)
                } else if (this.startsWith("[") && this.endsWith("]")) {
                    return GsonUtils.fromJson(this, List::class)
                }
            } catch (ignore: Exception) {
            }
        }
        return this
    }

    companion object {
        private val NUMERIC_PATTERN = Pattern.compile("^[\\-0-9][0-9]*(.[0-9]+)?\$")
        private val TRUE_OR_FALSE = arrayOf("true", "false")
    }
}