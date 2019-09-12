package com.itangcent.intellij.jvm.standard

import com.google.inject.Singleton
import com.intellij.psi.*
import com.itangcent.intellij.jvm.PsiResolver


@Singleton
class StandardPsiResolver : PsiResolver {

    override fun resolveRefText(psiExpression: PsiElement?): String? {
        when (psiExpression) {
            null -> return null
            is PsiLiteralExpression -> return psiExpression.value?.toString()
            is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                return resolveRefText(value)
            }
            is PsiField -> {
                val constantValue = psiExpression.computeConstantValue()
                if (constantValue != null) {
                    if (constantValue is PsiExpression) {
                        return resolveRefText(constantValue)
                    }
                    return constantValue.toString()
                }
                return psiExpression.text
            }
            else -> return psiExpression.text
        }
    }

}