package com.itangcent.intellij.psi

class ArrayDuckType : DuckType {
    val componentType: DuckType

    constructor(componentClass: DuckType) {
        this.componentType = componentClass
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