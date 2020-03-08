package com.itangcent.intellij.jvm

import com.intellij.psi.*
import com.itangcent.common.utils.KV
import com.itangcent.intellij.jvm.duck.DuckType

interface PsiClassHelper {

    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getTypeObject(duckType: DuckType?, context: PsiElement): Any?

    fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?>

    fun isNormalType(psiType: PsiType): Boolean

    fun isNormalType(psiClass: PsiClass): Boolean

    fun copy(obj: Any?): Any?

    fun unboxArrayOrList(psiType: PsiType): PsiType

    fun getDefaultValue(psiType: PsiType): Any?

    fun getDefaultValue(psiClass: PsiClass): Any?

    fun getJsonFieldName(psiField: PsiField): String

    fun getJsonFieldName(psiMethod: PsiMethod): String

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>>

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>>

    @Deprecated(message = "replace with [DocHelper#getAttrOfField]")
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

    fun isNormalType(duckType: DuckType): Boolean
    fun getDefaultValue(duckType: DuckType): Any?
}