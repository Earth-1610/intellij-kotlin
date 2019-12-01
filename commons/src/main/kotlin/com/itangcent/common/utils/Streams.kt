package com.itangcent.common.utils

import java.util.stream.Stream

/**
 * Returns a [List] containing all elements produced by this stream.
 */
@SinceKotlin("1.2")
inline fun <reified T> Stream<T>.toTypedArray(): Array<T> {
    return this.toArray<T> { i -> arrayOfNulls(i) }
}


inline fun <reified T> Stream<T>.skip(i: Int?): Stream<T> {
    if (i == null || i == 0) {
        return this
    }
    return this.skip(i.toLong())
}


fun Iterable<String?>.longest(): String? {
    return this
        .filter { it != null }
        .sortedByDescending { it!!.length }
        .firstOrNull()
}

fun Iterable<String?>.shortest(): String? {
    return this
        .filter { it != null }
        .sortedBy { it!!.length }
        .firstOrNull()
}
