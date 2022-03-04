package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiMethod
import com.itangcent.common.utils.mapToTypedArray
import com.itangcent.intellij.jvm.duck.DuckType

interface ExplicitMethod : DuckExplicitElement<PsiMethod> {

    /**
     * Returns the return type of the method.
     *
     * @return the method return type, or null if the method is a constructor.
     */
    fun getReturnType(): DuckType?

    /**
     * Returns the parameter list for the method.
     *
     * @return the parameter list instance.
     */
    fun getParameters(): Array<ExplicitParameter>

    /**
     * Searches the superclasses and base interfaces of the containing class to find
     * the methods which this method overrides or implements. Can return multiple results
     * if the base class and/or one or more of the implemented interfaces have a method
     * with the same signature. If the overridden method in turn overrides another method,
     * only the directly overridden method is returned.
     *
     * @return the array of super methods, or an empty array if no methods are found.
     */
    fun superMethods(): Array<ExplicitMethod>
}

class ExplicitMethodWithGenericInfo(
    private val containClass: ExplicitClass,
    private val psiMethod: PsiMethod
) : ExplicitElementWithGenericInfo<PsiMethod>(containClass), ExplicitMethod {

    override fun getReturnType(): DuckType? {
        val returnType = psiMethod.returnType ?: return null
        return ensureType(returnType)
    }

    override fun getParameters(): Array<ExplicitParameter> {
        val parameterList = psiMethod.parameterList.parameters
        if (parameterList.isEmpty()) return emptyArray()
        return parameterList.map { ExplicitParameterWithGenericInfo(this, it) }.toTypedArray()
    }

    override fun superMethods(): Array<ExplicitMethod> {
        val superMethods = psiMethod.findSuperMethods()
        if (superMethods.isNullOrEmpty()) {
            return emptyArray()
        }
        return containClass.methods().filter {
            superMethods.contains(it.psi())
        }.toTypedArray()
    }

    override fun psi(): PsiMethod {
        return psiMethod
    }

    override fun containClass(): ExplicitClass {
        return containClass.containClass()
    }

    override fun defineClass(): ExplicitClass {
        return containClass.defineClass()
    }

    override fun name(): String {
        return psiMethod.name
    }

    override fun toString(): String {
        return containClass().toString() + "#" + name()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitMethodWithGenericInfo

        if (containClass != other.containClass) return false
        if (psiMethod != other.psiMethod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containClass.hashCode()
        result = 31 * result + psiMethod.hashCode()
        return result
    }

}

class ExplicitMethodWithOutGenericInfo(
    private val containClass: ExplicitClass,
    private val psiMethod: PsiMethod
) : ExplicitElementWithOutGenericInfo<PsiMethod>(containClass), ExplicitMethod {

    override fun getReturnType(): DuckType? {
        val returnType = psiMethod.returnType ?: return null
        return ensureType(returnType)
    }

    override fun getParameters(): Array<ExplicitParameter> {
        val parameterList = psiMethod.parameterList.parameters
        if (parameterList.isEmpty()) return emptyArray()
        return parameterList.map { ExplicitParameterWithOutGenericInfo(this, it) }.toTypedArray()
    }

    /**
     * Searches the superclasses and base interfaces of the containing class to find
     * the methods which this method overrides or implements. Can return multiple results
     * if the base class and/or one or more of the implemented interfaces have a method
     * with the same signature. If the overridden method in turn overrides another method,
     * only the directly overridden method is returned.
     *
     * @return the array of super methods, or an empty array if no methods are found.
     */
    override fun superMethods(): Array<ExplicitMethod> {
        val superMethods = psiMethod.findSuperMethods()
        if (superMethods.isNullOrEmpty()) {
            return emptyArray()
        }
        return containClass.methods().filter {
            superMethods.contains(it.psi())
        }.toTypedArray()
    }

    override fun psi(): PsiMethod {
        return psiMethod
    }

    override fun containClass(): ExplicitClass {
        return containClass.containClass()
    }

    override fun defineClass(): ExplicitClass {
        return containClass.defineClass()
    }

    override fun name(): String {
        return psiMethod.name
    }

    override fun toString(): String {
        return containClass().toString() + "#" + name()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitMethodWithOutGenericInfo

        if (containClass != other.containClass) return false
        if (psiMethod != other.psiMethod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containClass.hashCode()
        result = 31 * result + psiMethod.hashCode()
        return result
    }
}