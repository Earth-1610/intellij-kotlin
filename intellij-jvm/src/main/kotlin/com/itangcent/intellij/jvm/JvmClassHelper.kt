package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.duck.SingleDuckType
import com.itangcent.intellij.jvm.element.ExplicitClass
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper

@ImplementedBy(StandardJvmClassHelper::class)
interface JvmClassHelper {

    fun isAccessibleField(field: PsiField): Boolean

    /**
     * Returns whether the given field is static.
     */
    fun isStatic(field: PsiField): Boolean

    /**
     * Returns whether the given field is static final.
     */
    fun isStaticFinal(field: PsiField): Boolean

    fun isMap(psiClass: PsiClass): Boolean

    fun isMap(psiType: PsiType): Boolean

    fun isMap(duckType: DuckType): Boolean

    fun isCollection(psiClass: PsiClass): Boolean

    fun isCollection(psiType: PsiType): Boolean

    fun isCollection(duckType: DuckType): Boolean

    fun isString(psiClass: PsiClass): Boolean

    fun isString(psiType: PsiType): Boolean

    fun isString(duckType: DuckType): Boolean

    fun isPublicStaticFinal(field: PsiField): Boolean

    /**
     * Returns whether the given {@code type} is a primitive or primitive wrapper
     * or {@code String}、{@code Object}
     */
    fun isNormalType(typeName: String): Boolean

    /**
     * Returns whether the given type is a primitive
     */
    fun isPrimitive(typeName: String): Boolean

    /**
     * Returns whether the given type is a primitive wrapper
     */
    fun isPrimitiveWrapper(typeName: String): Boolean

    fun isAccepted(oneClass: String, anotherClass: String): Boolean

    fun getDefaultValue(typeName: String): Any?

    fun isBasicMethod(methodName: String): Boolean

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    fun isEnum(psiType: PsiType): Boolean

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    fun isEnum(psiClass: PsiClass): Boolean

    /**
     * Checks if the class is an enumeration.
     *
     * @return true if the class is an enumeration, false otherwise.
     */
    fun isEnum(duckType: DuckType): Boolean

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    fun isInterface(psiType: PsiType): Boolean

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    fun isInterface(psiClass: PsiClass): Boolean

    /**
     * Checks if the class is an interface.
     *
     * @return true if the class is an interface, false otherwise.
     */
    fun isInterface(duckType: DuckType): Boolean

    fun resolveClassInType(psiType: PsiType): PsiClass?

    fun resolveClassToType(psiClass: PsiClass): PsiType?

    fun isInheritor(psiClass: PsiClass, vararg baseClass: String): Boolean

    fun isInheritor(psiType: PsiType, vararg baseClass: String): Boolean

    fun isInheritor(duckType: DuckType, vararg baseClass: String): Boolean

    /**
     * Returns the list of fields in the class and all its superclasses.
     *
     * @return the list of fields.
     */
    fun getAllFields(psiClass: PsiClass): Array<PsiField>

    /**
     * Returns the list of methods in the class and all its superclasses.
     *
     * @return the list of methods.
     */
    fun getAllMethods(psiClass: PsiClass): Array<PsiMethod>

    /**
     * Returns the list of methods in the class.
     *
     * @return the list of methods.
     */
    fun getMethods(psiClass: PsiClass): Array<PsiMethod>

    /**
     * Returns the list of fields in the class.
     *
     * @return the list of fields.
     */
    fun getFields(psiClass: PsiClass): Array<PsiField>

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

fun Any?.asPsiClass(): PsiClass? {
    this ?: return null
    return this.asPsiClass(ActionContext.instance(JvmClassHelper::class))
}

fun Any?.asPsiClass(jvmClassHelper: JvmClassHelper): PsiClass? {
    this ?: return null
    if (this is PsiType) {
        return jvmClassHelper.resolveClassInType(this)
    }
    if (this is DuckType) {
        return (this as? SingleDuckType)?.psiClass()
    }
    if (this is ExplicitClass) {
        return this.psi()
    }
    if (this is PsiClass) {
        return this
    }
    return null
}

fun JvmClassHelper.isAccepted(psiType: PsiType, anotherType: PsiType): Boolean {
    return psiType == anotherType || this.isAccepted(psiType.canonicalText, anotherType.canonicalText)
}