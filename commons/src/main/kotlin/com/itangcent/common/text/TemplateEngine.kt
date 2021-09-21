package com.itangcent.common.text

interface TemplateEngine {

    fun render(msg: String, placeholder: Array<Char>, templateEvaluator: TemplateEvaluator): String?
}