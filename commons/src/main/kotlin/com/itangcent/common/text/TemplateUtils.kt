package com.itangcent.common.text

import com.itangcent.common.spi.SpiUtils

object TemplateUtils {

    fun defaultTemplateEngine(): TemplateEngine {
        return SpiUtils.loadService(TemplateEngine::class) ?: DefaultTemplateEngine()
    }

    fun render(msg: String): TemplateRenderBuilder {
        return TemplateRenderBuilder(msg)
    }

    fun render(msg: String, context: Map<String?, *>): String? {
        return render(msg, TemplateEvaluator.from(context))
    }

    fun render(msg: String, supplier: (String) -> Any?): String? {
        return render(msg, TemplateEvaluator.from(supplier))
    }

    fun render(msg: String, placeholder: Array<Char>, context: Map<String?, *>): String? {
        return render(msg, placeholder, TemplateEvaluator.from(context))
    }

    fun render(msg: String, placeholder: Array<Char>, supplier: (String) -> Any?): String? {
        return render(msg, placeholder, TemplateEvaluator.from(supplier))
    }

    fun render(msg: String, templateEvaluator: TemplateEvaluator): String? {
        return defaultTemplateEngine().render(
            msg,
            DEFAULT_PLACEHOLDER,
            decorateTemplateEvaluator(templateEvaluator)
        )
    }

    fun render(msg: String, placeholder: Array<Char>, templateEvaluator: TemplateEvaluator): String? {
        return defaultTemplateEngine().render(
            msg,
            placeholder,
            decorateTemplateEvaluator(templateEvaluator)
        )
    }

    fun decorateTemplateEvaluator(templateEvaluator: TemplateEvaluator): TemplateEvaluator {
        val templateEvaluatorDecorators =
            SpiUtils.loadServices(TemplateEvaluatorDecorator::class) ?: return templateEvaluator
        var evaluator = templateEvaluator
        for (templateEvaluatorDecorator in templateEvaluatorDecorators) {
            evaluator = templateEvaluatorDecorator.decorate(evaluator)
        }
        return evaluator
    }
}

private val DEFAULT_PLACEHOLDER = arrayOf('$')

class TemplateRenderBuilder(private val msg: String) {

    private var handler: ((String, Any?) -> Unit)? = null

    private var templateEvaluator: TemplateEvaluator? = null

    private var placeholder: Array<Char>? = null

    /**
     * call back for handle the result of evaluate
     * @param handle (property,resolved)
     */
    fun onEval(handle: (String, Any?) -> Unit): TemplateRenderBuilder {
        this.handler = handle
        return this
    }

    fun context(context: Map<String?, *>): TemplateRenderBuilder {
        this.templateEvaluator = TemplateEvaluator.from(context)
        return this
    }

    fun context(supplier: (String) -> Any?): TemplateRenderBuilder {
        this.templateEvaluator = TemplateEvaluator.from(supplier)
        return this
    }

    fun templateEvaluator(templateEvaluator: TemplateEvaluator): TemplateRenderBuilder {
        this.templateEvaluator = templateEvaluator
        return this
    }

    fun placeholder(placeholder: Array<Char>): TemplateRenderBuilder {
        this.placeholder = placeholder
        return this
    }

    fun render(): String? {
        var evaluator = TemplateUtils.decorateTemplateEvaluator(templateEvaluator ?: TemplateEvaluator.nop())
        if (handler != null) {
            evaluator = TemplateEvaluatorWithHandler(evaluator, handler!!)
        }
        return TemplateUtils.defaultTemplateEngine().render(
            msg,
            placeholder ?: DEFAULT_PLACEHOLDER,
            evaluator
        )
    }
}

private class TemplateEvaluatorWithHandler(
    private val templateEvaluator: TemplateEvaluator,
    private val handler: (String, Any?) -> Unit
) : TemplateEvaluator {

    override fun eval(str: String): Any? {
        return templateEvaluator.eval(str).also {
            handler(str, it)
        }
    }
}