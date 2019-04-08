package com.itangcent.intellij.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.siyeh.ig.psiutils.ClassUtils

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

    fun findMethodFromFullName(fullName: String, context: PsiElement): PsiMethod? {

        val clsName = fullName.substringBefore("#")
        val cls = ClassUtils.findClass(clsName, context) ?: return null

        val methodAndParams = fullName.substringAfter("#")
        val method = methodAndParams.substringBefore("(")
        val candidates = cls.findMethodsByName(method, true)

        if (candidates.isEmpty()) {
            return null
        }

        if (candidates.size == 1) {
            return candidates[0]
        }

        for (candidate in candidates) {
            if (fullNameOfMethod(candidate) == fullName) {
                return candidate
            }
        }

        return null
    }

    fun findFieldFromFullName(fullName: String, context: PsiElement): PsiField? {
        val clsName = fullName.substringBefore("#")
        val cls = ClassUtils.findClass(clsName, context) ?: return null

        val fieldName = fullName.substringAfter("#")
        return cls.findFieldByName(fieldName, true)
    }
}