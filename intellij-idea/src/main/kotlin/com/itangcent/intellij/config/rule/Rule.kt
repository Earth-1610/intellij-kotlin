package com.itangcent.intellij.config.rule

typealias Rule<T> = (RuleContext) -> T?

fun <T> Rule<T>.filterWith(filter: Rule<Boolean>): Rule<T> {
    return { context ->
        if (filter(context) == true) {
            this@filterWith(context)
        } else {
            null
        }
    }
}

typealias AnyRule = Rule<Any>

typealias BooleanRule = Rule<Boolean>

typealias StringRule = Rule<String>

typealias EventRule = Rule<Unit>
fun BooleanRule.inverse(): BooleanRule {
    val origin = this
    return { context ->
        origin(context)?.let { !it }
    }
}