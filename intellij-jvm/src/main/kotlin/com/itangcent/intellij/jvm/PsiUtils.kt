package com.itangcent.intellij.jvm

import com.intellij.lang.jvm.JvmNamedElement
import com.intellij.psi.*

object PsiUtils {

    fun resolveExpr(expOrEle: Any): Any? {
        if (expOrEle is PsiExpression) {
            return resolveExpr(expOrEle)
        } else if (expOrEle is PsiElement) {
            return resolveExpr(expOrEle)
        }
        return expOrEle.toString()
    }

    fun resolveExpr(psiExpression: PsiExpression): Any? {
        when (psiExpression) {
            is PsiLiteralExpression -> return psiExpression.value
            is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                if (value != null) {
                    return resolveExpr(value)
                }
            }
            is JvmNamedElement -> return psiExpression.name
        }
        return psiExpression.text
    }

    fun resolveExpr(psiElement: PsiElement): Any? {
        when (psiElement) {
            is PsiVariable -> {
                val constantValue = psiElement.computeConstantValue()
                if (constantValue != null && constantValue != psiElement) {
                    return resolveExpr(constantValue)
                }
                return psiElement.name
            }
            is JvmNamedElement -> return psiElement.name
            is PsiExpression -> return resolveExpr(psiElement)
        }
        return psiElement.text
    }

}