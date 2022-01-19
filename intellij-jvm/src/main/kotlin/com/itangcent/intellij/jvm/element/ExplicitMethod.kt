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

    fun superMethods(): Array<ExplicitMethod>
}

class ExplicitMethodWithGenericInfo : ExplicitElementWithGenericInfo<PsiMethod>, ExplicitMethod {

    private val containClass: ExplicitClass

    private val psiMethod: PsiMethod

    constructor(containClass: ExplicitClass, psiMethod: PsiMethod) : super(containClass) {
        this.containClass = containClass
        this.psiMethod = psiMethod
    }

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
        return superMethods.mapToTypedArray { ExplicitMethodWithGenericInfo(containClass, it) }
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

class ExplicitMethodWithOutGenericInfo : ExplicitElementWithOutGenericInfo<PsiMethod>, ExplicitMethod {

    private val containClass: ExplicitClass

    private val psiMethod: PsiMethod

    constructor(containClass: ExplicitClass, psiMethod: PsiMethod) : super(containClass) {
        this.containClass = containClass
        this.psiMethod = psiMethod
    }

    override fun getReturnType(): DuckType? {
        val returnType = psiMethod.returnType ?: return null
        return ensureType(returnType)
    }

    override fun getParameters(): Array<ExplicitParameter> {
        val parameterList = psiMethod.parameterList.parameters
        if (parameterList.isEmpty()) return emptyArray()
        return parameterList.map { ExplicitParameterWithOutGenericInfo(this, it) }.toTypedArray()
    }

    override fun superMethods(): Array<ExplicitMethod> {
        val superMethods = psiMethod.findSuperMethods()
        if (superMethods.isNullOrEmpty()) {
            return emptyArray()
        }
        return superMethods.mapToTypedArray { ExplicitMethodWithOutGenericInfo(containClass, it) }
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