package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiParameter
import com.itangcent.intellij.jvm.duck.DuckType


interface ExplicitParameter : DuckExplicitElement<PsiParameter> {
    /**
     * Returns the type of the variable.
     *
     * @return the variable type.
     */
    fun getType(): DuckType?

    /**
     * Returns the method containing the member.
     *
     * @return the containing method.
     */
    fun containMethod(): ExplicitMethod
}

class ExplicitParameterWithGenericInfo : ExplicitElementWithGenericInfo<PsiParameter>, ExplicitParameter {
    private val containMethod: ExplicitMethod

    private val psiParameter: PsiParameter

    constructor(containMethod: ExplicitMethod, psiParameter: PsiParameter) : super(containMethod) {
        this.containMethod = containMethod
        this.psiParameter = psiParameter
    }

    override fun getType(): DuckType? {
        val returnType = psiParameter.type
        return ensureType(returnType)
    }

    override fun psi(): PsiParameter {
        return psiParameter
    }

    override fun containMethod(): ExplicitMethod {
        return containMethod
    }

    override fun containClass(): ExplicitClass {
        return containMethod.containClass()
    }

    override fun defineClass(): ExplicitClass {
        return containClass().defineClass()
    }

    override fun name(): String {
        return psiParameter.name
    }

    override fun toString(): String {
        return containMethod().toString() + "." + name()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitParameterWithGenericInfo

        if (containMethod != other.containMethod) return false
        if (psiParameter != other.psiParameter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containMethod.hashCode()
        result = 31 * result + psiParameter.hashCode()
        return result
    }

}

class ExplicitParameterWithOutGenericInfo : ExplicitElementWithOutGenericInfo<PsiParameter>, ExplicitParameter {

    private val containMethod: ExplicitMethod

    private val psiParameter: PsiParameter

    constructor(containMethod: ExplicitMethod, psiParameter: PsiParameter) : super(containMethod) {
        this.containMethod = containMethod
        this.psiParameter = psiParameter
    }

    override fun getType(): DuckType? {
        val returnType = psiParameter.type
        return ensureType(returnType)
    }

    override fun psi(): PsiParameter {
        return psiParameter
    }

    override fun containMethod(): ExplicitMethod {
        return containMethod
    }

    override fun containClass(): ExplicitClass {
        return containMethod.containClass()
    }

    override fun defineClass(): ExplicitClass {
        return containClass().defineClass()
    }

    override fun name(): String {
        return psiParameter.name
    }

    override fun toString(): String {
        return containMethod().toString() + "." + name()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitParameterWithOutGenericInfo

        if (containMethod != other.containMethod) return false
        if (psiParameter != other.psiParameter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containMethod.hashCode()
        result = 31 * result + psiParameter.hashCode()
        return result
    }
}
