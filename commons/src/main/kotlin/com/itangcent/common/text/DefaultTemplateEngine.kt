package com.itangcent.common.text

import java.util.regex.Pattern

class DefaultTemplateEngine : TemplateEngine {

    override fun render(msg: String, placeholder: CharArray, templateEvaluator: TemplateEvaluator): String {
        if (msg.isBlank()) return msg
        if (placeholder.none { msg.contains(it) }) return msg

        val pattern = Pattern.compile("[" + placeholder.joinToString(separator = "") + "]\\{(.*?)}")
        val match = pattern.matcher(msg)
        val sb = StringBuffer()
        while (match.find()) {
            val key = match.group(1)
            match.appendReplacement(sb, templateEvaluator.eval(key)?.toString() ?: "")
        }
        match.appendTail(sb)
        return sb.toString()
    }
}