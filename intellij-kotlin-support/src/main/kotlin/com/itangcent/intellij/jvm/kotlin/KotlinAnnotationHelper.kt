package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Singleton
import com.intellij.psi.*
import com.itangcent.intellij.jvm.standard.StandardAnnotationHelper
import org.jetbrains.kotlin.asJava.elements.KtLightPsiLiteral

@Singleton
class KotlinAnnotationHelper : StandardAnnotationHelper() {

    override fun resolveValue(psiExpression: PsiElement?): Any? {
        when (psiExpression) {
            null -> return null
            is KtLightPsiLiteral -> {
                val value = psiExpression.value
                if (value != null) {
                    if (value is String || value::class.java.isPrimitive) {
                        return value
                    }
                }
                return psiExpression.text
            }
            is PsiLiteralExpression -> return psiExpression.value?.toString()
            is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                if (value is PsiExpression) {
                    return resolveValue(value)
                }
            }
            is PsiField -> {
                val constantValue = psiExpression.computeConstantValue()
                if (constantValue != null) {
                    if (constantValue is PsiExpression) {
                        return resolveValue(constantValue)
                    }
                    return constantValue
                }
            }
            is PsiAnnotation -> {
                return annToMap(psiExpression)
            }
            is PsiArrayInitializerMemberValue -> {
                return psiExpression.initializers.map { resolveValue(it) }.toTypedArray()
            }
            else -> {
                return psiExpression.text
            }
        }

        return psiExpression.text
    }
}
