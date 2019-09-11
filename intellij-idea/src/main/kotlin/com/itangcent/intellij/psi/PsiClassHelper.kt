package com.itangcent.intellij.psi

import com.google.inject.ImplementedBy
import com.intellij.psi.*
import com.itangcent.intellij.util.KV


@ImplementedBy(DefaultPsiClassHelper::class)
interface PsiClassHelper {
    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?>

    fun isNormalType(typeName: String): Boolean

    fun copy(obj: Any?): Any?

    fun unboxArrayOrList(psiType: PsiType): PsiType

    fun getDefaultValue(typeName: String): Any?

    fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement?

    fun getJsonFieldName(psiField: PsiField): String

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>>

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>>

    fun getAttrOfField(field: PsiField): String?

    fun resolveEnumOrStatic(
        classNameWithProperty: String,
        psiMember: PsiMember,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>?

    fun resolveClass(className: String, psiMember: PsiMember): PsiClass?

    fun getContainingClass(psiMember: PsiMember): PsiClass?

}