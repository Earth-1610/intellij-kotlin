package com.itangcent.intellij.jvm.element

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.itangcent.intellij.jvm.DuckTypeHelper
import com.itangcent.intellij.jvm.duck.DuckType

interface ExplicitElement<E : PsiElement> {

    /**
     * Returns the PSI element which corresponds to this element
     *
     * @return the psi element
     */
    fun psi(): E

    /**
     * Returns the source PSI element of the [psi] element if this element have an associated file.
     * (For example, if the source code of a library is attached to a project, the source element for a compiled
     * library class is its source class.)
     *
     * @return the original element with source.
     */
    fun sourcePsi(): E {
        return psi()
    }

    /**
     * Returns the class containing the member.
     *
     * @return the containing class.
     */
    fun containClass(): ExplicitClass

    /**
     * Returns the name of the element.
     *
     * @return the element name.
     */
    fun name(): String
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