package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiField
import com.itangcent.intellij.jvm.duck.DuckType

interface ExplicitField : ExplicitElement<PsiField> {

    fun getType(): DuckType

    fun name(): String

    fun containClass(): ExplicitClass
}

class ExplicitFieldWithGenericInfo : ExplicitElementWithGenericInfo<PsiField>, ExplicitField {
    private val containClass: ExplicitClass

    private val psiField: PsiField

    constructor(containClass: ExplicitClass, psiField: PsiField) : super(containClass) {
        this.psiField = psiField
        this.containClass = containClass
    }

    override fun name(): String {
        return psiField.name
    }

    override fun getType(): DuckType {
        val returnType = psiField.type
        return ensureType(returnType) ?: duckTypeHelper.javaLangObjectType(psiField)
    }

    override fun containClass(): ExplicitClass {
        return containClass
    }

    override fun psi(): PsiField {
        return psiField
    }

}

class ExplicitFieldWithOutGenericInfo : ExplicitElementWithOutGenericInfo<PsiField>, ExplicitField {

    override fun name(): String {
        return psiField.name
    }

    private val containClass: ExplicitClass

    private val psiField: PsiField

    constructor(containClass: ExplicitClass, psiField: PsiField) : super(containClass) {
        this.containClass = containClass
        this.psiField = psiField
    }

    override fun getType(): DuckType {
        val returnType = psiField.type
        return ensureType(returnType) ?: duckTypeHelper.javaLangObjectType(psiField)
    }

    override fun containClass(): ExplicitClass {
        return containClass
    }

    override fun psi(): PsiField {
        return psiField
    }
}