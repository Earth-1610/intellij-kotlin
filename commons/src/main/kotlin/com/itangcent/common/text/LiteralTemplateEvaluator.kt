package com.itangcent.common.text

/**
 * resolve:
 * 'string' -> string
 * "string" -> string
 */
class LiteralTemplateEvaluator private constructor() : TemplateEvaluator {

    override fun eval(str: String): Any? {
        if (str.startsWith('\'') && str.endsWith('\'')) {
            return str.removeSurrounding("'")
        } else if (str.startsWith('\"') && str.endsWith('\"')) {
            return str.removeSurrounding("\"")
        }
        return null
    }

    companion object {
        val INSTANCE = LiteralTemplateEvaluator()
    }
}

class LiteralTemplateEvaluatorDecorator : TemplateEvaluatorDecorator {
    override fun decorate(templateEvaluator: TemplateEvaluator): TemplateEvaluator {
        return LiteralTemplateEvaluator.INSTANCE.union(templateEvaluator)
    }
}