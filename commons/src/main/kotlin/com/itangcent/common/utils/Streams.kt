package com.itangcent.common.utils

import java.util.stream.Stream

/**
 * Returns a [List] containing all elements produced by this stream.
 */
@SinceKotlin("1.2")
public inline fun <reified T> Stream<T>.toTypedArray(): Array<T> {
    return this.toArray<T> { i -> arrayOfNulls(i) }
}