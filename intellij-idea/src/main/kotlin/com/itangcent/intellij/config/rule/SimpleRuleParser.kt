package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.DocHelper
import java.util.regex.Pattern

@Singleton
class SimpleRuleParser : RuleParser {

    @Inject
    private val annotationHelper: AnnotationHelper? = null

    @Inject
    private val docHelper: DocHelper? = null

    private val stringRuleParseCache: HashMap<String, StringRule> = HashMap()

    override fun parseStringRule(rule: String): List<StringRule> {
        return parseStringRule(rule, "|")
    }

    override fun parseStringRule(rule: String, delimiters: String): List<StringRule> {
        return rule.split(delimiters)
            .mapNotNull { sr -> parseSingleStringRule(sr) }
            .toList()
    }

    fun parseSingleStringRule(rule: String): StringRule? {
        if (rule.isBlank()) return null

        if (stringRuleParseCache.containsKey(rule)) {
            return stringRuleParseCache[rule]
        }

        val tinyRuleStr = rule.trim()

        var srule: StringRule? = null

        if (tinyRuleStr.startsWith("@")) {
            val annStr = tinyRuleStr.substringAfter("@")
            val annName = annStr.substringBefore("#").trim()
            val annValue = annStr.substringAfter("#", "value").trim()
            srule = StringRule.of { context ->
                context.asPsiMember()?.let { annotationHelper!!.findAttr(it, annName, annValue) }
            }
        } else if (tinyRuleStr.startsWith("#")) {
            val tag = tinyRuleStr.substringAfter("#").trim()
            srule = StringRule.of { context ->
                docHelper!!.findDocsByTag(context.asPsiMember(), tag)
            }
        }

        if (srule != null) {
            stringRuleParseCache[rule] = srule
        }

        return srule
    }

    private val booleanRuleParseCache: HashMap<String, BooleanRule> = HashMap()

    override fun parseBooleanRule(rule: String): List<BooleanRule> {
        return parseBooleanRule(rule, "|", false)
    }

    override fun parseBooleanRule(rule: String, delimiters: String, defaultValue: Boolean): List<BooleanRule> {
        return rule.split(delimiters)
            .mapNotNull { sr -> parseSingleBooleanRule(sr, defaultValue) }
            .toList()
    }

    fun parseSingleBooleanRule(rule: String): BooleanRule? {
        return parseSingleBooleanRule(rule, false)
    }

    fun parseSingleBooleanRule(rule: String, defaultValue: Boolean): BooleanRule? {
        if (rule.isBlank()) return null

        if (booleanRuleParseCache.containsKey(rule)) {
            return booleanRuleParseCache[rule]
        }

        val tinyRuleStr = rule.trim()

        var srule: BooleanRule? = null

        if (tinyRuleStr.startsWith("!")) {
            val inverseRuleStr = tinyRuleStr.substring(1)
            val inverseRule: BooleanRule = parseSingleBooleanRule(inverseRuleStr, !defaultValue) ?: return null
            return inverseRule.inverse()
        } else if (tinyRuleStr.startsWith("@")) {
            val annStr = tinyRuleStr.substringAfter("@")
            val annName = annStr.substringBefore("#").trim()
            val annValue = annStr.substringAfter("#", "").trim()
            srule = if (annValue.isBlank()) {
                BooleanRule.of { context ->
                    context.asPsiMember()?.let { annotationHelper!!.hasAnn(it, annName) }
                }
            } else {
                BooleanRule.of { context ->
                    str2Bool(
                        context.asPsiMember()?.let { annotationHelper!!.findAttr(it, annName, annValue) },
                        defaultValue
                    )
                }
            }
        } else if (tinyRuleStr.startsWith("#")) {
            val tag = tinyRuleStr.substringAfter("#").trim()
            srule = BooleanRule.of { context ->
                docHelper!!.hasTag(context.asPsiMember(), tag)
            }
        } else if (tinyRuleStr.startsWith("$")) {
            val prefix = tinyRuleStr.substringBefore(":").trim()
            val content = tinyRuleStr.substringAfter(":", "")
            if (prefix == "\$class") {
                srule = if (content.startsWith("? extend")) {
                    val extendClass = content.removePrefix("? extend").trim()
                    val extendClassRegex = parseRegexOrConstant(extendClass)
                    BooleanRule.of { context ->
                        checkExtend(context.getResource()?.let { findClass(it) }, extendClassRegex)
                    }

                } else {
                    val contentRegex = parseRegexOrConstant(content)
                    BooleanRule.of { context ->
                        findClass(context.getResource())?.let { contentRegex(it.qualifiedName) } ?: false
                    }
                }

            }
        }

        if (srule != null) {
            booleanRuleParseCache[rule] = srule
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

    private fun findClass(named: PsiElement?): PsiClass? {
        return when (named) {
            null -> null
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
        return regexParseCache.computeIfAbsent(str) { _ ->
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

    override fun contextOf(psiElement: PsiElement): PsiElementContext {
        return when (psiElement) {
            is PsiClass -> PsiClassContext(psiElement)
            is PsiField -> PsiFieldContext(psiElement)
            is PsiMethod -> PsiMethodContext(psiElement)
            else -> PsiUnknownContext(psiElement)
        }
    }

    companion object {
        var STAR_DOT = "@S_T_A_R_D_O_T@"
        var STAR = "@O_N_L_Y_S_T_A_R@"
    }
}