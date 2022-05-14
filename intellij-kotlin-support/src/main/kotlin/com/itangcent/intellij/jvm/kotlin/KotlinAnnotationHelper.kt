package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.PsiElement
import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.longest
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.intellij.jvm.PsiExpressionResolver
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.ValueArgument

/**
 * see https://kotlinlang.org/docs/reference/annotations.html
 */
@Singleton
class KotlinAnnotationHelper : AnnotationHelper {

    @Inject
    private val fqNameHelper: FqNameHelper? = null

    @Inject(optional = true)
    private val psiClassHelper: PsiClassHelper? = null

    @Inject(optional = true)
    private val duckTypeHelper: DuckTypeHelper? = null

    @Inject
    protected lateinit var actionContext: ActionContext

    @Inject
    private val psiExpressionResolver: PsiExpressionResolver? = null

    override fun hasAnn(psiElement: PsiElement?, annName: String): Boolean {

        if (findKtAnnotation(psiElement, annName) != null) {
            return true
        }

        return false
    }

    @Suppress("UNCHECKED_CAST")
    override fun findAnnMap(psiElement: PsiElement?, annName: String): Map<String, Any?>? {
        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation != null) {
            val ret: LinkedHashMap<String, Any?> = LinkedHashMap()
            actionContext.callInReadUI {
                var allValue = 0
                ktAnnotation.valueArguments
                    .forEachIndexed { index, valueArgument ->
                        val argumentName = getArgName(psiElement, annName, valueArgument, index)
                        if (index == 0 && argumentName == "value") {
                            allValue = 1
                            ret[argumentName] =
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                        } else if (argumentName != null) {
                            allValue = -1
                            ret[argumentName] =
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                        } else if (allValue == 1) {
                            ++allValue
                            ret["value"] = arrayListOf(
                                ret["value"],
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                            )
                        } else if (allValue > 1) {
                            (ret["value"] as ArrayList<Any?>).add(
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                            )
                        }
                    }
//            annotationHelper.findAnnMap(psiElement, annName)?.forEach { k, v ->
//                ret.putIfAbsent(k, v)
//            }
            }
            return ret
        }

        return null
    }

    override fun findAnnMaps(psiElement: PsiElement?, annName: String): List<Map<String, Any?>>? {
        return findAnnMap(psiElement, annName)?.let { listOf(it) }
    }

    override fun findAttr(psiElement: PsiElement?, annName: String): Any? {
        return findAttr(psiElement, annName, "value")
    }

    @Suppress("UNCHECKED_CAST")
    override fun findAttr(psiElement: PsiElement?, annName: String, vararg attrs: String): Any? {
        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation == null) return null
        return actionContext.callInReadUI {
            if (attrs.contains("value")) {
                var allValue = 0
                var values: Any? = null
                ktAnnotation.valueArguments
                    .forEachIndexed { index, valueArgument ->
                        val argumentName = getArgName(psiElement, annName, valueArgument, index)
                        if (index == 0 && argumentName == "value") {
                            allValue = 1
                            values =
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                        } else if (argumentName != null) {
                            allValue = -1
                            if (attrs.contains(argumentName)) {
                                return@callInReadUI valueArgument.getArgumentExpression()
                                    ?.let { psiExpressionResolver!!.process(it) }
                            }
                        } else if (allValue == 1) {
                            ++allValue
                            values = arrayListOf(
                                values,
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                            )
                        } else if (allValue > 1) {
                            (values as ArrayList<Any?>).add(
                                valueArgument.getArgumentExpression()?.let { psiExpressionResolver!!.process(it) }
                            )
                        }
                    }
                return@callInReadUI values
            } else {
                return@callInReadUI ktAnnotation.valueArguments
                    .filterIndexed { index, valueArgument ->
                        getArgName(psiElement, annName, valueArgument, index)?.let { name ->
                            attrs.contains(name)
                        } == true
                    }
                    .mapNotNull { it.getArgumentExpression() }
                    .map { psiExpressionResolver!!.process(it) }
                    .firstOrNull()
            }
        }
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String): String? {
        return findAttrAsString(psiElement, annName, "value")
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String, vararg attrs: String): String? {
        val ktAnnotation = findKtAnnotation(psiElement, annName) ?: return null
        return actionContext.callInReadUI {
            ktAnnotation.valueArguments
                .filterIndexed { index, valueArgument ->
                    getArgName(psiElement, annName, valueArgument, index)?.let { name ->
                        attrs.contains(name)
                    } == true
                }
                .mapNotNull { it.getArgumentExpression() }
                .mapNotNull { psiExpressionResolver!!.process(it) }
                .mapNotNull { tinyAnnStr(it) }
                .longest()
        }
    }

    private fun getArgName(psiElement: PsiElement?, annName: String, valArg: ValueArgument?, index: Int): String? {
        var name = valArg?.getArgumentName()?.asName?.asString()
        if (name.isNullOrBlank()) {
            name = findDefaultParamName(psiElement, annName, index)
        }
        return name
    }

    private fun findDefaultParamName(psiElement: PsiElement?, annName: String, index: Int): String? {

        if (psiClassHelper == null) {
            return null
        }

        if (psiElement == null) {
            return null
        }

        val resolveClass = duckTypeHelper!!.resolveClass(annName, psiElement) ?: return null
        if (resolveClass is KtLightClass) {
            val parameters = resolveClass.kotlinOrigin?.getPrimaryConstructorParameterList()?.parameters ?: return null
            if (parameters.size > index) {
                return parameters[index]?.name
            }
        }

        if (index == 0) {
            return "value"
        }

        //only named arguments are available for java annotations
//        return Stream.of(*resolveClass.methods)
//            .map { it.name }
//            .skip(index - 1)
//            .findFirst()
//            .orElse(null)
        return null
    }

    private fun findKtAnnotation(psiElement: PsiElement?, annName: String): KtAnnotationEntry? {

        if (psiElement is KtLightMember<*>) {
            val kotlinOrigin = psiElement.kotlinOrigin
            if (kotlinOrigin != null) {
                return actionContext.callInReadUI {
                    return@callInReadUI kotlinOrigin.findAnnotation(fqNameHelper!!.of(annName))
                }
            }
        }

        if (psiElement is KtLightClassForSourceDeclaration) {
            val kotlinOrigin = psiElement.kotlinOrigin
            return actionContext.callInReadUI {
                return@callInReadUI kotlinOrigin.findAnnotation(fqNameHelper!!.of(annName))
            }
        }

        if (psiElement is KtDeclaration) {
            return actionContext.callInReadUI {
                return@callInReadUI psiElement.findAnnotation(fqNameHelper!!.of(annName))
            }
        }
        return null
    }

    fun tinyAnnStr(annStr: Any?): String? {
        return when (annStr) {
            null -> null
            is Array<*> -> annStr.joinToString(separator = "\n")
            is Collection<*> -> annStr.joinToString(separator = "\n")
            is String -> annStr
            else -> GsonUtils.toJson(annStr)
        }
    }
}