package com.itangcent.common.utils

import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * @sample samples.collections.Collections.Transformations.joinToString
 */
public fun <T> Stream<T>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * @sample samples.collections.Collections.Transformations.joinTo
 */
public fun <T, A : Appendable> Stream<T>.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            buffer.appendElement(element, transform)
        } else break
    }
    if (limit in 0..(count - 1)) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

internal fun <T> Appendable.appendElement(element: T, transform: ((T) -> CharSequence)?) {
    when {
        transform != null -> append(transform(element))
        element is CharSequence? -> append(element)
        element is Char -> append(element)
        else -> append(element.toString())
    }
}


fun <T> Stream<T>.firstOrNull(): T? {
    return this.findFirst().orElse(null)
}

fun <T> Stream<T>.firstOrNull(predicate: (T) -> Boolean): T? {
    return this.filter(predicate).firstOrNull()
}

fun <T> kotlin.Array<T>?.stream(): java.util.stream.Stream<T> {
    if (this.isNullOrEmpty()) {
        return Stream.empty()
    }
    return Stream.of(*this!!)
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
inline fun <S, T : S> Stream<T>.reduceSafely(operation: (acc: S, T) -> S): S? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return null
    var accumulator: S = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
    }
    return accumulator
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> Stream<*>.filterAs(): Stream<T> {
    return this
        .filter { it != null && it is T }
        .map { it as T }
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Stream<*>.filterAs(cls: KClass<T>): Stream<T> {
    return this
        .filter { it != null && cls.isInstance(it) }
        .map { it as T }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> MutableList<*>.filterAs(): MutableList<T> {
    val list = ArrayList<T>()
    for (any in this) {
        if (any != null && any is T) {
            list.add(any)
        }
    }
    return list
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> MutableList<*>.filterAs(cls: KClass<T>): MutableList<T> {
    val list = ArrayList<T>()
    for (any in this) {
        if (any != null && cls.isInstance(any)) {
            list.add(any as T)
        }
    }
    return list
}

/**
 * Returns a array containing the results of applying the given [transform] function
 * to each element in the original array.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, reified R> Array<out T>.mapToTypedArray(transform: (T) -> R): Array<R> {
    val array = Array<R?>(this.size) { null }
    for ((index, t) in this.withIndex()) {
        array[index] = transform(t)
    }
    return array as Array<R>
}

/**
 * Returns a array containing the results of applying the given [transform] function
 * to each element in the original list.
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, reified R> Collection<T>.mapToTypedArray(transform: (T) -> R): Array<R> {
    val array = Array<R?>(this.size) { null }
    for ((index, t) in this.withIndex()) {
        array[index] = transform(t)
    }
    return array as Array<R>
}

@Suppress("UNCHECKED_CAST")
fun <T> Stream<T?>.filterNotNull(): Stream<T> {
    return this
        .filter { it != null }
        .map { it!! }
}

@Suppress("UNCHECKED_CAST")
fun <T, R> Stream<T?>.mapNotNull(transform: (T) -> R?): Stream<R> {
    return this
        .filter { it != null }
        .map { transform(it!!) }
        .filter { it != null }
        .map { it!! }
}
