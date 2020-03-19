package com.itangcent.intellij.jvm.duck

interface DuckType {

    fun isSingle(): Boolean

    fun canonicalText(): String

    fun name(): String

    fun unbox(): DuckType
}