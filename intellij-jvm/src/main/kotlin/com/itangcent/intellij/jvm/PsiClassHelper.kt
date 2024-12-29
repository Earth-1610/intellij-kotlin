package com.itangcent.intellij.jvm

import com.intellij.psi.*
import com.itangcent.common.utils.Extensible
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.element.ExplicitElement
import com.itangcent.intellij.jvm.element.ExplicitField
import com.itangcent.intellij.jvm.element.ExplicitMethod

interface PsiClassHelper {

    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getTypeObject(duckType: DuckType?, context: PsiElement): Any?

    fun getTypeObject(duckType: DuckType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?, context: PsiElement?): Map<String, Any?>

    fun getFields(psiClass: PsiClass?, context: PsiElement?, option: Int): Map<String, Any?>

    fun getFields(psiClass: PsiClass?): Map<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): Map<String, Any?>

    fun isNormalType(psiType: PsiType): Boolean

    fun isNormalType(psiClass: PsiClass): Boolean

    fun unboxArrayOrList(psiType: PsiType): PsiType

    fun getDefaultValue(psiType: PsiType): Any?

    fun getDefaultValue(psiClass: PsiClass): Any?

    fun getJsonFieldName(psiField: PsiField): String

    fun getJsonFieldName(psiMethod: PsiMethod): String

    fun getJsonFieldName(accessibleField: AccessibleField): String

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>>

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>>

    fun resolveEnumOrStatic(
        classNameWithProperty: String,
        context: PsiElement,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>?

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

    fun Int.has(flag: Int): Boolean {
        return (this and flag) != 0
    }

    fun Int.hasAny(vararg flag: Int): Boolean {
        return flag.any { (this and it) != 0 }
    }
}

/**
 * Represents an accessible field in a class.
 *
 * An `AccessibleField` extends the `Extensible` interface, which allows
 * for attaching additional attributes to the field.
 *
 * @property field The explicit field representation, or `null` if not available.
 * @property getter The explicit getter method representation, or `null` if not available.
 * @property setter The explicit setter method representation, or `null` if not available.
 * @property name The name of the field.
 * @property type The type of the field.
 * @property psi The PSI element representing the field.
 */
interface AccessibleField : Extensible {
    val field: ExplicitField?
    var getter: ExplicitMethod?
    var setter: ExplicitMethod?

    val name: String
    val type: DuckType
    val psi: PsiElement
    val explicitElement: ExplicitElement<*>
}
