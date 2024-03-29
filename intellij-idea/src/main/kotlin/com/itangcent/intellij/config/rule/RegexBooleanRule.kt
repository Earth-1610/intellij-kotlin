package com.itangcent.intellij.config.rule

import java.util.*
import java.util.regex.Pattern

abstract class RegexBooleanRule(protected val keyRegexStr: String) : BooleanRule {

    protected abstract fun match(name: String): Pair<Boolean, Map<Int, String>?>?

    fun match(context: RuleContext): Pair<Boolean, Map<Int, String>?>? {
        return match(context.toString())
    }

    override fun invoke(context: RuleContext): Boolean? {
        return match(context)?.first
    }

    fun asFilterOf(stringRule: StringRule): StringRule {
        return { context ->
            val match = this.match(context)
            if (match?.first == true) {
                renderVal(stringRule(context), match.second)
            } else {
                null
            }
        }
    }

    abstract fun renderVal(str: String?, second: Map<Int, String>?): String?

    companion object {
        fun compile(regexStr: String, value: String): RegexBooleanRule {
            val keyRegexStr = regexStr.removePrefix("#regex:")
            val matcherValue = Pattern.compile("\\$\\{(\\d+)}").matcher(value)
            val valueGroups: LinkedList<Int> = LinkedList()
            while (matcherValue.find()) {
                valueGroups.add(matcherValue.group(1).toInt())
            }
            return if (valueGroups.isEmpty()) {
                SimpleRegexBooleanRule(keyRegexStr)
            } else {
                GroupedRegexBooleanRule(keyRegexStr, valueGroups.toTypedArray())
            }
        }
    }

    class SimpleRegexBooleanRule(keyRegexStr: String) : RegexBooleanRule(keyRegexStr) {
        override fun renderVal(str: String?, second: Map<Int, String>?): String? {
            return str
        }

        override fun match(name: String): Pair<Boolean, Map<Int, String>?>? {
            val matcher = Pattern.compile(keyRegexStr).matcher(name)
            return when {
                matcher.matches() -> true to null
                else -> false to null
            }
        }

    }

    class GroupedRegexBooleanRule(keyRegexStr: String, private val valueGroups: Array<Int>) :
        RegexBooleanRule(keyRegexStr) {
        override fun renderVal(str: String?, second: Map<Int, String>?): String? {
            if (str.isNullOrBlank()) return str
            var ret: String = str
            for (valueGroup in valueGroups) {
                ret = ret.replace("\${$valueGroup}", second?.get(valueGroup) ?: "")
            }
            return ret
        }

        override fun match(name: String): Pair<Boolean, Map<Int, String>?>? {
            val matcher = Pattern.compile(keyRegexStr).matcher(name)
            return if (matcher.matches()) {
                val ret: HashMap<Int, String> = HashMap(valueGroups.size)
                for (valueGroup in valueGroups) {
                    ret[valueGroup] = matcher.group(valueGroup)
                }
                true to ret
            } else {
                false to null
            }
        }
    }
}