package com.itangcent.intellij.config

import com.google.inject.Singleton
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.itangcent.intellij.config.context.PsiElementContext
import com.itangcent.intellij.psi.PsiAnnotationUtils
import com.itangcent.intellij.util.DocCommentUtils
import java.util.regex.Pattern

@Singleton
class SimpleRuleParse {

    private val simpleStringRuleParseCache: HashMap<String, SimpleStringRule> = HashMap()

    fun parseStringRule(rule: String): List<SimpleStringRule> {
        return parseStringRule(rule, "|")
    }

    fun parseStringRule(rule: String, delimiters: String): List<SimpleStringRule> {
        return rule.split(delimiters)
            .mapNotNull { sr -> parseSingleStringRule(sr) }
            .toList()
    }

    fun parseSingleStringRule(rule: String): SimpleStringRule? {
        if (rule.isBlank()) return null

        if (simpleStringRuleParseCache.containsKey(rule)) {
            return simpleStringRuleParseCache[rule]
        }

        val tinyRuleStr = rule.trim()

        var srule: SimpleStringRule? = null

        if (tinyRuleStr.startsWith("@")) {
            val annStr = tinyRuleStr.substringAfter("@")
            val annName = annStr.substringBefore("#").trim()
            val annValue = annStr.substringAfter("#", "value").trim()
            srule = { ann ->
                PsiAnnotationUtils.findAttr(ann.asPsiDocCommentOwner(), annName, annValue)
            }
        } else if (tinyRuleStr.startsWith("#")) {
            val tag = tinyRuleStr.substringAfter("#").trim()
            srule = { context ->
                DocCommentUtils.findDocsByTag(context.asPsiDocCommentOwner().docComment, tag)
            }
        }

        if (srule != null) {
            simpleStringRuleParseCache[rule] = srule
        }

        return srule
    }

    private val simpleBooleanRuleParseCache: HashMap<String, SimpleBooleanRule> = HashMap()

    fun parseBooleanRule(rule: String): List<SimpleBooleanRule> {
        return parseBooleanRule(rule, "|", false)
    }

    fun parseBooleanRule(rule: String, delimiters: String, defaultValue: Boolean): List<SimpleBooleanRule> {
        return rule.split(delimiters)
            .mapNotNull { sr -> parseSingleBooleanRule(sr, defaultValue) }
            .toList()
    }

    fun parseSingleBooleanRule(rule: String): SimpleBooleanRule? {
        return parseSingleBooleanRule(rule, false)
    }

    fun parseSingleBooleanRule(rule: String, defaultValue: Boolean): SimpleBooleanRule? {
        if (rule.isBlank()) return null

        if (simpleBooleanRuleParseCache.containsKey(rule)) {
            return simpleBooleanRuleParseCache[rule]
        }

        val tinyRuleStr = rule.trim()

        var srule: SimpleBooleanRule? = null

        if (tinyRuleStr.startsWith("!")) {
            val inverseRuleStr = tinyRuleStr.substring(1)
            val inverseRule: SimpleBooleanRule = parseSingleBooleanRule(inverseRuleStr, !defaultValue) ?: return null
            return { context ->
                !inverseRule(context)
            }
        } else if (tinyRuleStr.startsWith("@")) {
            val annStr = tinyRuleStr.substringAfter("@")
            val annName = annStr.substringBefore("#").trim()
            val annValue = annStr.substringAfter("#", "").trim()
            srule = if (annValue.isBlank()) {
                { context ->
                    PsiAnnotationUtils.findAnn(context.asPsiDocCommentOwner(), annName) != null
                }
            } else {
                { context ->
                    str2Bool(
                        PsiAnnotationUtils.findAttr(context.asPsiDocCommentOwner(), annName, annValue),
                        defaultValue
                    )
                }
            }
        } else if (tinyRuleStr.startsWith("#")) {
            val tag = tinyRuleStr.substringAfter("#").trim()
            srule = { context ->
                DocCommentUtils.hasTag(context.asPsiDocCommentOwner().docComment, tag)
            }
        } else if (tinyRuleStr.startsWith("$")) {
            val prefix = tinyRuleStr.substringBefore(":").trim()
            val content = tinyRuleStr.substringAfter(":", "")
            if (prefix == "\$class") {
                if (content.startsWith("? extend")) {
                    val extendClass = content.removePrefix("? extend").trim()
                    val extendClassRegex = parseRegexOrConstant(extendClass)
                    srule = { context ->
                        checkExtend(findClass(context.getResource()), extendClassRegex)
                    }

                } else {
                    val contentRegex = parseRegexOrConstant(content)
                    srule = { context ->
                        findClass(context.getResource())?.let { contentRegex(it.qualifiedName) } ?: false
                    }
                }

            }
        }

        if (srule != null) {
            simpleBooleanRuleParseCache[rule] = srule
        }

        return srule
    }

    private fun checkExtend(cls: PsiClass?, extendClassRegex: (String?) -> Boolean): Boolean {
        if (cls == null) return false
        var _cls: PsiClass? = cls
        do {
            if (extendClassRegex(_cls!!.qualifiedName)) {
                return true
            }
            _cls = _cls.superClass
        } while (_cls != null && _cls.name != "Object")
        return false

    }

    private fun findClass(named: PsiElement): PsiClass? {
        return when (named) {
            is PsiClass -> named
            is PsiMember -> named.containingClass
            else -> null
        }
    }

    private fun str2Bool(str: String?, defaultValue: Boolean): Boolean {
        if (str.isNullOrBlank()) return defaultValue

        return try {
            str.toBoolean()
        } catch (e: Exception) {
            defaultValue
        }

    }

    private val regexParseCache: HashMap<String, (String?) -> Boolean> = HashMap()

    fun parseRegexOrConstant(str: String): (String?) -> Boolean {
        return regexParseCache.computeIfAbsent(str) {
            if (str.isBlank()) {
                return@computeIfAbsent { true }
            }
            val tinyStr = str.trim()
            if (tinyStr == "*") {
                return@computeIfAbsent { true }
            }

            if (tinyStr.contains("*")) {
                val pattern = Pattern.compile(
                    "^${
                    tinyStr.replace("*.", STAR_DOT)
                        .replace("*", STAR)
                        .replace(STAR_DOT, ".*?(?<=^|\\.)")
                        .replace(STAR, ".*?")
                        .replace("[", "\\[")
                        .replace("]", "\\]")

                    }$"
                )

                return@computeIfAbsent {
                    pattern.matcher(it).matches()
                }
            }

            return@computeIfAbsent {
                str == it
            }
        }
    }

    companion object {
        var STAR_DOT = "@S_T_A_R_D_O_T@"
        var STAR = "@O_N_L_Y_S_T_A_R@"
    }
}

typealias SimpleStringRule = (PsiElementContext) -> String?
typealias SimpleBooleanRule = (PsiElementContext) -> Boolean
