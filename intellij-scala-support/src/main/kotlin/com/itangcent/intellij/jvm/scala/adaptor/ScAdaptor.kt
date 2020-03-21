package com.itangcent.intellij.jvm.scala.adaptor

import com.itangcent.common.utils.cast
import kotlin.reflect.KClass

interface ScAdaptor<T> {

    fun adaptor(): T
}

fun <T : Any> Any.tryCast(kclass: KClass<T>): T? {
    return this.cast(kclass) ?: (this as? ScAdaptor<*>)?.adaptor().cast(kclass)
}