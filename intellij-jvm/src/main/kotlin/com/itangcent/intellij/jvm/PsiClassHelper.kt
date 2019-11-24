package com.itangcent.intellij.jvm

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiType
import com.itangcent.common.utils.KV

interface PsiClassHelper {

    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?>

    fun isNormalType(psiType: PsiType): Boolean

    fun isNormalType(psiClass: PsiClass): Boolean

    fun copy(obj: Any?): Any?

    fun unboxArrayOrList(psiType: PsiType): PsiType

    fun getDefaultValue(psiType: PsiType): Any?

    fun getDefaultValue(psiClass: PsiClass): Any?

    fun getJsonFieldName(psiField: PsiField): String

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>>

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>>

    fun getAttrOfField(field: PsiField): String?

    fun resolveEnumOrStatic(
        classNameWithProperty: String,
        context: PsiElement,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>?

    @Deprecated(
        "use [com.itangcent.intellij.jvm.PsiClassHelper.resolveEnumOrStatic(com.intellij.psi.PsiClass, java.lang.String, java.lang.String)]",
        ReplaceWith("resolveEnumOrStatic(cls, property, property ?: \"\")")
    )
    fun resolveEnumOrStatic(cls: PsiClass?, property: String?): ArrayList<HashMap<String, Any?>>? {
        return resolveEnumOrStatic(cls, property, property ?: "")
    }

    fun resolveEnumOrStatic(
        cls: PsiClass?, property: String?,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>?
}