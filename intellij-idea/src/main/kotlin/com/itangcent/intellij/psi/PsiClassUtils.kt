package com.itangcent.intellij.psi

import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.siyeh.ig.psiutils.ClassUtils

object PsiClassUtils {

    fun isInterface(psiType: PsiType): Boolean {
        return PsiTypesUtil.getPsiClass(psiType)?.isInterface ?: false
    }

    fun hasImplement(psiClass: PsiClass?, interCls: PsiClass?): Boolean {
        if (psiClass == null || interCls == null) {
            return false
        }
        if (psiClass == interCls || psiClass.isInheritor(interCls, true)) {
            return true
        }
        val qualifiedName = interCls.qualifiedName
        if (psiClass.qualifiedName == qualifiedName) {
            return true
        }
        if (psiClass.isInterface) {
            var from = psiClass.superClass
            while (from != null) {
                if (from == psiClass || from.qualifiedName == qualifiedName) {
                    return true
                }

                from = from.superClass
            }
            return false
        } else {
            return psiClass.interfaces.any { it == interCls || it.qualifiedName == qualifiedName }
        }
    }

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