package com.itangcent.intellij.config.rule

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.itangcent.common.utils.toBool
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.duck.SingleUnresolvedDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.element.ExplicitElement
import com.itangcent.intellij.jvm.element.ExplicitField
import com.itangcent.intellij.jvm.element.ExplicitMethod
import java.util.regex.Pattern

@Singleton
class SimpleRuleParser : RuleParser {

    @Inject
    private val annotationHelper: AnnotationHelper? = null

    @Inject
    private val docHelper: DocHelper? = null

    @Inject
    private val jvmClassHelper: JvmClassHelper? = null

    private val stringRuleParseCache: HashMap<String, StringRule> = HashMap()

    override fun parseStringRule(rule: String): StringRule? {
        if (rule.isBlank()) return null

        if (stringRuleParseCache.containsKey(rule)) {
            return stringRuleParseCache[rule]
        }

        val tinyRuleStr = rule.trim()

        val srule: StringRule = when {
            tinyRuleStr.startsWith("@") -> {
                val annStr = tinyRuleStr.substringAfter("@")
                val annName = annStr.substringBefore("#").trim()
                val annValue = annStr.substringAfter("#", "value").trim()
                StringRule.of { context ->
                    context.getResource()?.let {
                        annotationHelper!!.findAttrAsString(it, annName, annValue)
                    }
                }
            }
            tinyRuleStr.startsWith("#") -> {
                val tag = tinyRuleStr.substringAfter("#").trim()
                StringRule.of { context ->
                    docHelper!!.findDocByTag(context.getResource(), tag)
                }
            }
            tinyRuleStr.startsWith("~") -> {
                val suffix = tinyRuleStr.removePrefix("~")
                StringRule.of { it.getName() + suffix }
            }
            else -> StringRule.of { tinyRuleStr }
        }

        stringRuleParseCache[rule] = srule

        return srule
    }

    private val booleanRuleParseCache: HashMap<String, BooleanRule> = HashMap()

    override fun parseBooleanRule(rule: String): BooleanRule? {
        return parseBooleanRule(rule, false)
    }

    fun parseBooleanRule(rule: String, defaultValue: Boolean): BooleanRule? {
        if (rule.isBlank()) return null

        if (booleanRuleParseCache.containsKey(rule + defaultValue)) {
            return booleanRuleParseCache[rule + defaultValue]
        }

        val tinyRuleStr = rule.trim()

        var srule: BooleanRule? = null

        if (tinyRuleStr.startsWith("!")) {
            val inverseRuleStr = tinyRuleStr.substring(1)
            val inverseRule: BooleanRule = parseBooleanRule(inverseRuleStr, !defaultValue) ?: return null
            return inverseRule.inverse()
        } else if (tinyRuleStr.startsWith("@")) {
            val annStr = tinyRuleStr.substringAfter("@")
            val annName = annStr.substringBefore("#").trim()
            val annValue = annStr.substringAfter("#", "").trim()
            srule = if (annValue.isBlank()) {
                BooleanRule.of { context ->
                    context.getResource()?.let { annotationHelper!!.hasAnn(it, annName) }
                }
            } else {
                BooleanRule.of { context ->
                    context.getResource()?.let { annotationHelper!!.findAttrAsString(it, annName, annValue) }
                        ?.toBool(defaultValue)
                }
            }
        } else if (tinyRuleStr.startsWith("#")) {
            val tag = tinyRuleStr.substringAfter("#").trim()
            srule = BooleanRule.of { context ->
                docHelper!!.hasTag(context.getResource(), tag)
            }
        } else if (tinyRuleStr.startsWith("\$class:")) {
            val content = tinyRuleStr.removePrefix("\$class:")
            if (content.isEmpty()) {
                return null
            }

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

        } else {
            //default =
            srule = BooleanRule.of { context ->
                context.getName() == rule
            }
        }

        booleanRuleParseCache[rule] = srule

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

                return@computeIfAbsent { s ->
                    pattern.matcher(s).matches()
                }
            }

            return@computeIfAbsent { s -> str == s }
        }
    }

    override fun contextOf(target: Any, context: PsiElement?): RuleContext {
        return when (target) {
            is PsiClass -> PsiClassContext(target)
            is PsiField -> PsiFieldContext(target)
            is PsiMethod -> PsiMethodContext(target)
            is PsiElement -> UnknownPsiElementContext(target)
            is ExplicitElement<*> -> {
                return when (target) {
                    is ExplicitClass -> ExplicitClassContext(target)
                    is ExplicitMethod -> ExplicitMethodContext(target)
                    is ExplicitField -> ExplicitFieldContext(target)
                    else -> CompanionUnknownPsiElementContext(target, target.psi())
                }
            }
            is PsiType -> PsiTypeContext(
                target,
                jvmClassHelper!!.resolveClassInType(target)
            )
            is DuckType -> {
                return when (target) {
                    is SingleDuckType -> {
                        PsiClassContext(target.psiClass())
                    }
                    is SingleUnresolvedDuckType -> {
                        PsiTypeContext(
                            target.psiType(),
                            jvmClassHelper!!.resolveClassInType(target.psiType())
                        )
                    }
                    else -> {
                        DuckTypeContext(
                            target,
                            null
                        )
                    }
                }
            }
            is String -> StringRuleContext(
                target,
                context!!
            )
            else -> throw IllegalArgumentException("unable to build context of:$target")
        }
    }

    companion object {
        var STAR_DOT = "@S_T_A_R_D_O_T@"
        var STAR = "@O_N_L_Y_S_T_A_R@"
    }
}