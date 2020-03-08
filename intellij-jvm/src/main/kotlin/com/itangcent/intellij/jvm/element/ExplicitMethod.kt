package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiMethod
import com.itangcent.intellij.jvm.duck.DuckType

interface ExplicitMethod : DuckExplicitElement<PsiMethod> {

    fun getReturnType(): DuckType?

    fun getParameters(): Array<ExplicitParameter>

    fun containClass(): ExplicitClass

    fun name(): String
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
}