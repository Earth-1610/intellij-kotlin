package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiMethod
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

    override fun psi(): PsiMethod {
        return psiMethod
    }

    override fun containClass(): ExplicitClass {
        return containClass
    }

    override fun name(): String {
        return psiMethod.name
    }

    override fun toString(): String {
        return name()
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

    override fun psi(): PsiMethod {
        return psiMethod
    }

    override fun containClass(): ExplicitClass {
        return containClass
    }

    override fun name(): String {
        return psiMethod.name
    }

    override fun toString(): String {
        return name()
    }
}