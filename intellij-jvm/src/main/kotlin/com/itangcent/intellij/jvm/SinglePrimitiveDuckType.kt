package com.itangcent.intellij.jvm

import com.intellij.psi.PsiType

class SinglePrimitiveDuckType : DuckType {
    fun psiType(): PsiType {
        return psiType
    }

    private val psiType: PsiType

    constructor(psiType: PsiType) {
        this.psiType = psiType
    }

    override fun canonicalText(): String {
        return psiType.canonicalText
    }

    override fun toString(): String {
        return psiType.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SinglePrimitiveDuckType

        if (psiType != other.psiType) return false

        return true
    }

    override fun hashCode(): Int {
        return psiType.hashCode()
    }


}