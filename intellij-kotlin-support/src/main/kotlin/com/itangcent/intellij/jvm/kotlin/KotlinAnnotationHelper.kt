package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.psi.*
import com.itangcent.common.utils.invokeMethod
import com.itangcent.common.utils.longest
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.intellij.jvm.PsiResolver
import com.itangcent.intellij.jvm.standard.StandardAnnotationHelper
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.plainContent

/**
 * see https://kotlinlang.org/docs/reference/annotations.html
 */
@Singleton
class KotlinAnnotationHelper : StandardAnnotationHelper() {

    @Inject
    private val fqNameHelper: FqNameHelper? = null

    @Inject(optional = true)
    private val psiClassHelper: PsiClassHelper? = null

    @Inject(optional = true)
    private val psiResolver: PsiResolver? = null

    override fun hasAnn(psiElement: PsiElement?, annName: String): Boolean {

        if (findKtAnnotation(psiElement, annName) != null) {
            return true
        }

        return super.hasAnn(psiElement, annName)
    }

    override fun findAnnMap(psiElement: PsiElement?, annName: String): Map<String, Any?>? {
        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation != null) {
            val ret: LinkedHashMap<String, Any?> = LinkedHashMap()
            ktAnnotation.valueArguments
                .forEachIndexed { index, valueArgument ->
                    val argumentName = getArgName(psiElement, annName, valueArgument, index)
                    if (argumentName != null) {
                        ret[argumentName] = resolveValue(valueArgument.getArgumentExpression())
                    }
                }
//            super.findAnnMap(psiElement, annName)?.forEach { k, v ->
//                ret.putIfAbsent(k, v)
//            }
            return ret
        }

        return super.findAnnMap(psiElement, annName)
    }


    override fun findAttr(psiElement: PsiElement?, annName: String): Any? {
        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation != null) {
            return ktAnnotation.valueArguments
                .map { resolveValue(it.getArgumentExpression()) }
                .firstOrNull()
        }

        return super.findAttr(psiElement, annName)
    }

    override fun findAttr(psiElement: PsiElement?, annName: String, vararg attrs: String): Any? {

        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation != null) {
            return ktAnnotation.valueArguments
                .filterIndexed { index, valueArgument ->
                    getArgName(psiElement, annName, valueArgument, index)?.let { name -> attrs.contains(name) } == true
                }
                .map { resolveValue(it.getArgumentExpression()) }
                .firstOrNull()
        }


        return super.findAttr(psiElement, annName, *attrs)
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String): String? {
        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation != null) {
            return ktAnnotation.valueArguments
                .map { resolveValue(it.getArgumentExpression()) }
                .map { tinyAnnStr(it) }
                .longest()
        }

        return super.findAttrAsString(psiElement, annName)
    }

    override fun findAttrAsString(psiElement: PsiElement?, annName: String, vararg attrs: String): String? {
        val ktAnnotation = findKtAnnotation(psiElement, annName)
        if (ktAnnotation != null) {
            return ktAnnotation.valueArguments
                .filterIndexed { index, valueArgument ->
                    getArgName(psiElement, annName, valueArgument, index)?.let { name -> attrs.contains(name) } == true
                }
                .map { resolveValue(it.getArgumentExpression()) }
                .map { tinyAnnStr(it) }
                .longest()
        }

        return super.findAttrAsString(psiElement, annName, *attrs)
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

        val resolveClass = psiResolver!!.resolveClass(annName, psiElement) ?: return null
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
                return kotlinOrigin.findAnnotation(fqNameHelper!!.of(annName))
            }
        }

        if (psiElement is KtLightClassForSourceDeclaration) {
            val kotlinOrigin = psiElement.kotlinOrigin
            return kotlinOrigin.findAnnotation(fqNameHelper!!.of(annName))
        }

        if (psiElement is KtDeclaration) {
            return psiElement.findAnnotation(fqNameHelper!!.of(annName))
        }

        return null
    }

    override fun resolveValue(psiExpression: PsiElement?): Any? {
        when {
            psiExpression == null -> return null
            CompatibleKtClass.isKtLightPsiLiteral(psiExpression) -> {
                val value = psiExpression.invokeMethod("getValue")
                if (value != null) {
                    if (value is String || value::class.java.isPrimitive) {
                        return value
                    }
                }
                return psiExpression.text
            }
            psiExpression is PsiLiteralExpression -> return psiExpression.value?.toString()
            psiExpression is KtCollectionLiteralExpression -> {
                return psiExpression.getInnerExpressions().map { resolveValue(it) }.toTypedArray()
            }
            psiExpression is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                if (value is PsiExpression) {
                    return resolveValue(value)
                }
            }
            psiExpression is PsiField -> {
                val constantValue = psiExpression.computeConstantValue()
                if (constantValue != null) {
                    if (constantValue is PsiExpression) {
                        return resolveValue(constantValue)
                    }
                    return constantValue
                }
            }
            psiExpression is PsiAnnotation -> {
                return annToMap(psiExpression)
            }
            psiExpression is PsiArrayInitializerMemberValue -> {
                return psiExpression.initializers.map { resolveValue(it) }.toTypedArray()
            }
            psiExpression is KtStringTemplateExpression -> {
                return psiExpression.plainContent
            }
            else -> {
                return psiExpression.text
            }
        }

        return psiExpression.text
    }
}