package com.itangcent.intellij.jvm.duck

import com.intellij.psi.PsiType

class SingleUnresolvedDuckType : DuckType {

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

    override fun isSingle(): Boolean {
        return true
    }

    override fun toString(): String {
        return psiType.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleUnresolvedDuckType

        if (psiType != other.psiType) return false

        return true
    }

    override fun hashCode(): Int {
        return psiType.hashCode()
    }

    override fun name(): String {
        return psiType.presentableText
    }

    override fun unbox(): DuckType {
        return this
    }
}