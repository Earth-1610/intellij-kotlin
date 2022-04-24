package com.itangcent.intellij.jvm

import com.intellij.psi.*
import com.itangcent.common.utils.KV
import com.itangcent.intellij.jvm.duck.DuckType

interface PsiClassHelper {

    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getTypeObject(duckType: DuckType?, context: PsiElement): Any?

    fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?, context: PsiElement?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, context: PsiElement?, option: Int): KV<String, Any?>

    fun getFields(psiClass: PsiClass?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?>

    fun isNormalType(psiType: PsiType): Boolean

    fun isNormalType(psiClass: PsiClass): Boolean

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
        ReplaceWith("resolveEnumOrStatic(cls, property, property)")
    )
    fun resolveEnumOrStatic(
        context: PsiElement, cls: PsiClass?, property: String?
    ): ArrayList<HashMap<String, Any?>>? {
        return resolveEnumOrStatic(context, cls, property, "")
    }

    fun resolveEnumOrStatic(
        context: PsiElement,
        cls: PsiClass?, property: String?,
        defaultPropertyName: String,
        valueTypeHandle: ((DuckType) -> Unit)? = null
    ): ArrayList<HashMap<String, Any?>>?

    fun isNormalType(duckType: DuckType): Boolean

    fun getDefaultValue(duckType: DuckType): Any?
}

object JsonOption {
    const val NONE = 0b0000//None additional options
    const val READ_COMMENT = 0b0001//try find comments
    const val READ_GETTER = 0b0010//Try to find the available getter method as property
    const val READ_SETTER = 0b0100//Try to find the available setter method as property
    const val ALL = READ_COMMENT or READ_GETTER or READ_SETTER//All additional options
    const val READ_GETTER_OR_SETTER =
        READ_GETTER or READ_SETTER//Try to find the available getter or setter method as property

    @Deprecated(
        message = "use #has",
        replaceWith = ReplaceWith("has(JsonOption.READ_COMMENT)")
    )
    fun needComment(flag: Int): Boolean {
        return (flag and READ_COMMENT) != 0
    }

    @Deprecated(
        message = "use #has",
        replaceWith = ReplaceWith("has(JsonOption.READ_GETTER)")
    )
    fun readGetter(flag: Int): Boolean {
        return (flag and READ_GETTER) != 0
    }

    fun Int.has(flag: Int): Boolean {
        return (this and flag) != 0
    }

    fun Int.hasAny(vararg flag: Int): Boolean {
        return flag.any { (this and it) != 0 }
    }
}