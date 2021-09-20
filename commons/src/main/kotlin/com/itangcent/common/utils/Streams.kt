package com.itangcent.common.utils

import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

object Streams {

    fun <T> fromGenerate(generator: () -> T?): Stream<T> {
        return StreamSupport.stream(Iterables.asIterable(generator).spliterator(), false)
    }
}

/**
 * Returns a [Array] containing all elements produced by this stream.
 */
@SinceKotlin("1.2")
inline fun <reified T> Stream<T>.toTypedArray(): Array<T> {
    return this.toArray { i -> arrayOfNulls(i) }
}

/**
 * Returns a stream consisting of the remaining elements of this stream
 * after discarding the first [i] elements of the stream.
 * If this stream contains fewer than [i] elements then an
 * empty stream will be returned.
 *
 * @param i the number of leading elements to skip
 * @return the new stream
 * @throws IllegalArgumentException if [i] is negative
 */
inline fun <reified T> Stream<T>.skip(i: Int?): Stream<T> {
    if (i == null || i == 0) {
        return this
    }
    return this.skip(i.toLong())
}

/**
 * Returns the longest string, or `null` if the iterable is empty.
 */
fun Iterable<String>?.longest(): String? {
    return this.head { it.length }
}

/**
 * Returns the shortest string, or `null` if the iterable is empty.
 */
fun Iterable<String>?.shortest(): String? {
    return this.tail { it.length }
}

/**
 * Returns the longest string, or `null` if the array is empty.
 */
fun Array<String>?.longest(): String? {
    return this.head { it.length }
}

/**
 * Returns the shortest string, or `null` if the array is empty.
 */
fun Array<String>?.shortest(): String? {
    return this.tail { it.length }
}

/**
 * Returns the longest string, or `null` if the stream is empty.
 */
fun Stream<String>?.longest(): String? {
    return this.head { it.length }
}

/**
 * Returns the shortest string, or `null` if the stream is empty.
 */
fun Stream<String>?.shortest(): String? {
    return this.tail { it.length }
}

/**
 * Returns the head element sorted by [selector], or `null` if the iterable is empty.
 */
inline fun <T, R : Comparable<R>> Iterable<T>?.head(crossinline selector: (T) -> R?): T? {
    if (this == null) {
        return null
    } else if (this is Collection) {
        if (this.isEmpty()) {
            return null
        } else if (this.size == 1) {
            return this.first()
        }
    }
    return this
        .filterNotNull()
        .sortedByDescending { selector(it!!) }
        .firstOrNull()
}

/**
 * Returns the tail element sorted by [selector], or `null` if the iterable is empty.
 */
inline fun <T, R : Comparable<R>> Iterable<T>?.tail(crossinline selector: (T) -> R?): T? {
    if (this == null) {
        return null
    } else if (this is Collection) {
        if (this.isEmpty()) {
            return null
        } else if (this.size == 1) {
            return this.first()
        }
    }
    return this
        .filterNotNull()
        .sortedBy { selector(it!!) }
        .firstOrNull()
}

/**
 * Returns the head element sorted by [selector], or `null` if the array is empty.
 */
inline fun <T, R : Comparable<R>> Array<T>?.head(crossinline selector: (T) -> R?): T? {
    return when {
        this.isNullOrEmpty() -> null
        this.size == 1 -> this[0]
        else -> this
            .filterNotNull()
            .sortedByDescending { selector(it!!) }
            .firstOrNull()
    }
}

/**
 * Returns the tail element sorted by [selector], or `null` if the array is empty.
 */
inline fun <T, R : Comparable<R>> Array<T>?.tail(crossinline selector: (T) -> R?): T? {
    return when {
        this.isNullOrEmpty() -> null
        this.size == 1 -> this[0]
        else -> this
            .filterNotNull()
            .sortedBy { selector(it!!) }
            .firstOrNull()
    }
}

/**
 * Returns the head element sorted by [selector], or `null` if the stream is null.
 */
inline fun <T, R : Comparable<R>> Stream<T>?.head(crossinline selector: (T) -> R): T? {
    return when {
        this == null -> null
        else -> this
            .filter { it != null }
            .sorted(Comparator.comparing<T, R?> { selector(it) }.reversed())
            .firstOrNull()
    }
}

/**
 * Returns the tail element sorted by [selector], or `null` if the stream is null.
 */
inline fun <T, R : Comparable<R>> Stream<T>?.tail(crossinline selector: (T) -> R): T? {
    return when {
        this == null -> null
        else -> this
            .filter { it != null }
            .sorted(Comparator.comparing<T, R?> { selector(it) })
            .firstOrNull()
    }
}
