package com.itangcent.common.text

object GroovyScriptEngineTemplateEvaluator {
    fun evaluator(): TemplateEvaluator {
        return DefaultGroovyScriptEngineTemplateEvaluator()
    }

    fun with(templateEvaluator: TemplateEvaluator): TemplateEvaluator {
        return GroovyScriptEngineTemplateEvaluatorWithSuperTemplateEvaluator(templateEvaluator)
    }
}

private class DefaultGroovyScriptEngineTemplateEvaluator : ScriptEngineTemplateEvaluator() {
    override fun scriptType(): String {
        return "groovy"
    }
}

private class GroovyScriptEngineTemplateEvaluatorWithSuperTemplateEvaluator(templateEvaluator: TemplateEvaluator) :
    ScriptEngineTemplateEvaluatorWithSuperTemplateEvaluator(
        templateEvaluator
    ) {

    override fun scriptType(): String {
        return "groovy"
    }
}

class GroovyScriptEngineTemplateEvaluatorDecorator : TemplateEvaluatorDecorator {
    override fun decorate(templateEvaluator: TemplateEvaluator): TemplateEvaluator {
        return templateEvaluator.union(GroovyScriptEngineTemplateEvaluator.with(templateEvaluator))
    }
}