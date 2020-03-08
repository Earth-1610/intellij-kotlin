package com.itangcent.intellij.jvm.duck

import com.itangcent.intellij.jvm.DuckTypeHelper

class ArrayDuckType : DuckType {

    private val componentType: DuckType

    constructor(componentClass: DuckType) {
        this.componentType = componentClass
    }

    override fun canonicalText(): String {
        return componentType.canonicalText() + DuckTypeHelper.ARRAY_SUFFIX
    }

    override fun isSingle(): Boolean {
        return false
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

    override fun name(): String {
        return componentType.name() + DuckTypeHelper.ARRAY_SUFFIX
    }

    override fun unbox(): DuckType {
        return componentType.unbox()
    }
}