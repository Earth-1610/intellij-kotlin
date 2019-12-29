package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
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
}