package com.itangcent.intellij.jvm

import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiClassUtil

class ArrayDuckType : DuckType {

    private val componentType: DuckType

    constructor(componentClass: DuckType) {
        this.componentType = componentClass
    }

    fun componentType(): DuckType {
        return componentType
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayDuckType

        if (componentType != other.componentType) return false

        return true
    }

    override fun hashCode(): Int {
        return componentType.hashCode()
    }

}