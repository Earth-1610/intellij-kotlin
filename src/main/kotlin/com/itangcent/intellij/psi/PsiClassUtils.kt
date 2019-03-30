package com.itangcent.intellij.psi

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

object PsiClassUtils {

    fun fullNameOfMethod(psiMethod: PsiMethod): String {
        val sb = StringBuilder()
        psiMethod.containingClass?.let { sb.append(it.qualifiedName).append("#") }
        sb.append(psiMethod.name)
        sb.append("(")
        var first = true
        for (parameter in psiMethod.parameterList.parameters) {
            when {
                first -> first = false
                else -> sb.append(",")
            }
            sb.append(parameter.type.canonicalText)
        }
        sb.append(")")
        return sb.toString()
    }

    fun fullNameOfField(psiField: PsiField): String {
        val sb = StringBuilder()
        psiField.containingClass?.let { sb.append(it.qualifiedName).append("#") }
        sb.append(psiField.name)
        return sb.toString()
    }
}