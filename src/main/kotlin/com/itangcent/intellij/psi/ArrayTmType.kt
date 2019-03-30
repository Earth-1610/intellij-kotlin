package com.itangcent.intellij.psi

class ArrayTmType : TmType {
    val componentType: TmType

    constructor(componentClass: TmType) {
        this.componentType = componentClass
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayTmType

        if (componentType != other.componentType) return false

        return true
    }

    override fun hashCode(): Int {
        return componentType.hashCode()
    }

}