package com.itangcent.intellij.file

interface BeanBinder<T : Any> {
    fun tryRead(): T?

    fun read(): T

    fun save(t: T?)
}