package com.itangcent.intellij.jvm.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import com.siyeh.ig.psiutils.ClassUtils
import java.util.ArrayDeque

object PsiClassUtil {

    fun isInterface(psiType: PsiType): Boolean {
        return PsiTypesUtil.getPsiClass(psiType)?.isInterface == true
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
        return fullNameOfMethod(psiMethod.containingClass, psiMethod)
    }

    fun fullNameOfMethod(psiClass: PsiClass?, psiMethod: PsiMethod): String {
        val sb = StringBuilder()
        psiClass?.let { sb.append(it.qualifiedName).append("#") }
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
        return fullNameOfField(psiField.containingClass, psiField)
    }

    fun fullNameOfField(psiClass: PsiClass?, psiField: PsiField): String {
        val sb = StringBuilder()
        psiClass?.let { sb.append(it.qualifiedName).append("#") }
        sb.append(psiField.name)
        return sb.toString()
    }

    fun findMethodFromFullName(fullName: String, context: PsiElement): PsiMethod? {

        val clsName = fullName.findClassName()
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
        val clsName = fullName.findClassName()
        val cls = ClassUtils.findClass(clsName, context) ?: return null

        val fieldName = fullName.substringAfter("#")
        return cls.findFieldByName(fieldName, true)
    }

    fun fullNameOfMember(psiClass: PsiClass?, psiMember: PsiElement): String {
        if (psiMember is PsiMethod) {
            return fullNameOfMethod(psiClass, psiMember)
        }
        if (psiMember is PsiField) {
            return fullNameOfField(psiClass, psiMember)
        }
        return nameOfMember(psiMember)
    }

    fun fullNameOfMember(psiMember: PsiElement): String {
        if (psiMember is PsiMethod) {
            return fullNameOfMethod(psiMember)
        }
        if (psiMember is PsiField) {
            return fullNameOfField(psiMember)
        }
        return nameOfMember(psiMember)
    }

    fun qualifiedNameOfMethod(psiMethod: PsiMethod): String {
        return qualifiedNameOfMethod(psiMethod.containingClass, psiMethod)
    }

    fun qualifiedNameOfMethod(psiClass: PsiClass?, psiMethod: PsiMethod): String {
        val sb = StringBuilder()
        psiClass?.let { sb.append(it.qualifiedName).append("#") }
        sb.append(psiMethod.name)
        return sb.toString()
    }

    fun qualifiedNameOfField(psiField: PsiField): String {
        return qualifiedNameOfField(psiField.containingClass, psiField)
    }

    fun qualifiedNameOfField(psiClass: PsiClass?, psiField: PsiField): String {
        val sb = StringBuilder()
        psiClass?.let { sb.append(it.qualifiedName).append("#") }
        sb.append(psiField.name)
        return sb.toString()
    }

    fun findMethodFromQualifiedName(qualifiedName: String, context: PsiElement): PsiMethod? {
        val clsName = qualifiedName.findClassName()
        val cls = ClassUtils.findClass(clsName, context) ?: return null

        val methodAndParams = qualifiedName.substring(clsName.length + 1)
        val method = methodAndParams.substringBefore("(")
        val candidates = cls.findMethodsByName(method, true)

        if (candidates.isEmpty()) {
            return null
        }

        if (candidates.size == 1) {
            return candidates[0]
        }

        for (candidate in candidates) {
            if (qualifiedNameOfMethod(candidate) == qualifiedName) {
                return candidate
            }
        }

        return null
    }

    fun findFieldFromQualifiedName(qualifiedName: String, context: PsiElement): PsiField? {
        val clsName = qualifiedName.findClassName()
        val cls = ClassUtils.findClass(clsName, context) ?: return null

        val fieldName = qualifiedName.substring(clsName.length + 1)
        return cls.findFieldByName(fieldName, true)
    }

    fun qualifiedNameOfMember(psiClass: PsiClass?, psiMember: PsiElement): String {
        if (psiMember is PsiMethod) {
            return qualifiedNameOfMethod(psiClass, psiMember)
        }
        if (psiMember is PsiField) {
            return qualifiedNameOfField(psiClass, psiMember)
        }
        return nameOfMember(psiMember)
    }

    fun qualifiedNameOfMember(psiElement: PsiElement): String {
        if (psiElement is PsiMethod) {
            return qualifiedNameOfMethod(psiElement)
        }
        if (psiElement is PsiField) {
            return qualifiedNameOfField(psiElement)
        }
        return nameOfMember(psiElement)
    }

    fun nameOfMember(psiElement: PsiElement): String {
        if (psiElement is PsiMethod) {
            return psiElement.name
        }
        if (psiElement is PsiField) {
            return psiElement.name
        }
        if (psiElement is PsiParameter) {
            return psiElement.name
        }
        if (psiElement is PsiNamedElement) {
            return psiElement.name ?: "anonymous"
        }
        return "anonymous"
    }

    fun logicalNameOfMember(psiElement: PsiElement): String {
        if (psiElement is PsiMethod) {
            return psiElement.name + "()"
        }
        if (psiElement is PsiField) {
            return psiElement.name
        }
        if (psiElement is PsiParameter) {
            return psiElement.name
        }
        if (psiElement is PsiNamedElement) {
            return psiElement.name ?: "anonymous"
        }
        return "anonymous"
    }

    private fun String.findClassName(): String {
        return if (this.contains('#')) {
            this.substringBefore('#')
        } else {
            this.substringBefore('.')
        }
    }

    fun getPackageNameOf(psiClass: PsiClass): String? {
        val className = psiClass.qualifiedName ?: return null
        return className.substringBeforeLast('.')
    }

    fun getAllMethods(psiClass: PsiClass): List<PsiMethod> {
        val result = mutableListOf<PsiMethod>()

        // Add all methods from the class itself
        result.addAll(psiClass.methods)

        // Add methods from superclasses
        var superClass = psiClass.superClass
        while (superClass != null) {
            result.addAll(superClass.methods)
            superClass = superClass.superClass
        }

        // Add methods from interfaces
        val processedInterfaces = mutableSetOf<String>()
        val interfacesToProcess = ArrayDeque<PsiClass>()

        // Add direct interfaces
        psiClass.interfaces.forEach { interfacesToProcess.add(it) }

        // Process interfaces recursively
        while (interfacesToProcess.isNotEmpty()) {
            val currentInterface = interfacesToProcess.removeFirst()
            val qualifiedName = currentInterface.qualifiedName ?: continue

            if (processedInterfaces.contains(qualifiedName)) {
                continue
            }

            processedInterfaces.add(qualifiedName)
            result.addAll(currentInterface.methods)

            // Add super interfaces
            currentInterface.interfaces.forEach { interfacesToProcess.add(it) }
        }

        return result
    }
}