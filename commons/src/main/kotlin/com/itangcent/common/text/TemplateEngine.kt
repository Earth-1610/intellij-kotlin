package com.itangcent.common.text

interface TemplateEngine {

    fun render(msg: String, placeholder: CharArray, templateEvaluator: TemplateEvaluator): String?
}