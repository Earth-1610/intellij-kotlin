package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiParameter
import com.itangcent.intellij.jvm.duck.DuckType


interface ExplicitParameter : DuckExplicitElement<PsiParameter> {

    fun getType(): DuckType?

    fun containMethod(): ExplicitMethod

    fun name(): String
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

    override fun name(): String {
        return psiParameter.name ?: ""
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

    override fun name(): String {
        return psiParameter.name ?: ""
    }
}
