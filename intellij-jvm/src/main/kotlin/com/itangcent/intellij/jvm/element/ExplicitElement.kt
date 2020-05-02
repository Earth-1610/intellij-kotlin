package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.itangcent.intellij.jvm.duck.DuckType
import com.itangcent.intellij.jvm.DuckTypeHelper

interface ExplicitElement<E : PsiElement> {

    fun psi(): E

    fun containClass(): ExplicitClass
}

interface DuckExplicitElement<E : PsiElement> : ExplicitElement<E> {

    fun duckTypeHelper(): DuckTypeHelper

    fun genericInfo(): Map<String, DuckType?>?
}


abstract class ExplicitElementWithGenericInfo<E : PsiElement> : DuckExplicitElement<E> {
    protected val duckTypeHelper: DuckTypeHelper
    private val genericInfo: Map<String, DuckType?>?//generic type info

    constructor(
        duckTypeHelper: DuckTypeHelper,
        genericInfo: Map<String, DuckType?>?
    ) {
        this.genericInfo = genericInfo
        this.duckTypeHelper = duckTypeHelper
    }

    constructor(parent: DuckExplicitElement<*>) {
        this.duckTypeHelper = parent.duckTypeHelper()
        this.genericInfo = parent.genericInfo()
    }

    fun ensureType(psiType: PsiType): DuckType? {
        return duckTypeHelper.ensureType(psiType, genericInfo)
    }

    override fun duckTypeHelper(): DuckTypeHelper {
        return duckTypeHelper
    }

    override fun genericInfo(): Map<String, DuckType?>? {
        return genericInfo
    }
}

abstract class ExplicitElementWithOutGenericInfo<E : PsiElement> : DuckExplicitElement<E> {
    protected val duckTypeHelper: DuckTypeHelper

    constructor(
        duckTypeHelper: DuckTypeHelper
    ) {
        this.duckTypeHelper = duckTypeHelper
    }

    constructor(parent: DuckExplicitElement<*>) {
        this.duckTypeHelper = parent.duckTypeHelper()
    }

    fun ensureType(psiType: PsiType): DuckType? {
        return duckTypeHelper.ensureType(psiType)
    }

    override fun duckTypeHelper(): DuckTypeHelper {
        return duckTypeHelper
    }

    override fun genericInfo(): Map<String, DuckType?>? {
        return null
    }
}