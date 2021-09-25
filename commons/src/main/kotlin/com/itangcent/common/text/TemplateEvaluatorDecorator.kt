package com.itangcent.common.text

interface TemplateEvaluatorDecorator {
    fun decorate(templateEvaluator: TemplateEvaluator): TemplateEvaluator
}