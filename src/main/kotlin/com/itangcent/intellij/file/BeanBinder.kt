package com.itangcent.intellij.file

interface BeanBinder<T : Any> {
    fun read(): T
    fun save(t: T)
}