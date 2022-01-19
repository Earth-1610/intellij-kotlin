package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiField
import com.itangcent.intellij.jvm.duck.DuckType

interface ExplicitField : ExplicitElement<PsiField> {

    fun getType(): DuckType
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
        return containClass.containClass()
    }

    override fun defineClass(): ExplicitClass {
        return containClass.defineClass()
    }

    override fun psi(): PsiField {
        return psiField
    }

    override fun toString(): String {
        return containClass().toString() + "#" + name()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitFieldWithGenericInfo

        if (containClass != other.containClass) return false
        if (psiField != other.psiField) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containClass.hashCode()
        result = 31 * result + psiField.hashCode()
        return result
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
        return containClass.containClass()
    }

    override fun defineClass(): ExplicitClass {
        return containClass.defineClass()
    }

    override fun psi(): PsiField {
        return psiField
    }

    override fun toString(): String {
        return containClass().toString() + "#" + name()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplicitFieldWithOutGenericInfo

        if (containClass != other.containClass) return false
        if (psiField != other.psiField) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containClass.hashCode()
        result = 31 * result + psiField.hashCode()
        return result
    }
}