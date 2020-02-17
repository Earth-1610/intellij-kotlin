package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.*
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper

@ImplementedBy(StandardJvmClassHelper::class)
interface JvmClassHelper {

    fun isAccessibleField(field: PsiField): Boolean

    fun isStaticFinal(field: PsiField): Boolean

    fun isMap(psiClass: PsiClass): Boolean

    fun isMap(psiType: PsiType): Boolean

    fun isCollection(psiClass: PsiClass): Boolean

    fun isCollection(psiType: PsiType): Boolean

    fun isPublicStaticFinal(field: PsiField): Boolean

    fun isNormalType(typeName: String): Boolean

    fun getDefaultValue(typeName: String): Any?

    fun isBasicMethod(methodName: String): Boolean

    fun isEnum(psiType: PsiType): Boolean

    fun isEnum(psiClass: PsiClass): Boolean

    fun resolveClassInType(psiType: PsiType): PsiClass?

    fun resolveClassToType(psiClass: PsiClass): PsiType?

    fun isInheritor(psiClass: PsiClass, vararg baseClass: String): Boolean

    fun isInheritor(psiType: PsiType, vararg baseClass: String): Boolean

    fun getAllFields(psiClass: PsiClass): Array<PsiField>

    fun getAllMethods(psiClass: PsiClass): Array<PsiMethod>

    fun extractModifiers(psiElement: PsiElement): List<String>

    /**
     * define code without implement code
     */
    fun defineCode(psiElement: PsiElement): String {
        return when (psiElement) {
            is PsiClass -> defineClassCode(psiElement)
            is PsiMethod -> defineMethodCode(psiElement)
            is PsiField -> defineFieldCode(psiElement)
            is PsiParameter -> defineParamCode(psiElement)
            else -> defineOtherCode(psiElement)
        }
    }

    /**
     * define code without class body
     */
    fun defineClassCode(psiClass: PsiClass): String

    /**
     * define code without method body
     */
    fun defineMethodCode(psiMethod: PsiMethod): String

    /**
     * define code without get/set
     */
    fun defineFieldCode(psiField: PsiField): String

    /**
     * define code
     */
    fun defineParamCode(psiParameter: PsiParameter): String

    /**
     * other psi element
     */
    fun defineOtherCode(psiElement: PsiElement): String

}