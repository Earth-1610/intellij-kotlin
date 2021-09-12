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
        return psiParameter.name ?: ""
    }

    override fun toString(): String {
        return containMethod().toString() + "." + name()
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
        return psiParameter.name ?: ""
    }

    override fun toString(): String {
        return containMethod().toString() + "." + name()
    }
}
