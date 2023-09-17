package com.itangcent.common.utils

import java.util.*

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
inline fun <S, T : S> Iterable<T>.reduceSafely(operation: (acc: S, T) -> S): S? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return null
    var accumulator: S = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
    }
    return accumulator
}

fun Array<*>?.notNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

fun Collection<*>?.notNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

fun <E> List<E>.asArrayList(): ArrayList<E> {
    if (this is ArrayList<E>) {
        return this
    }
    return ArrayList(this)
}

private val IMMUTABLE_LIST_CLASSES = listOf(
    emptyList<Any>().javaClass,
    Collections.emptyList<Any>().javaClass,
    Collections.singletonList("").javaClass,
)

private val IMMUTABLE_SET_CLASSES = listOf(
    emptySet<Any>().javaClass,
    Collections.emptySet<Any>().javaClass,
)

private val IMMUTABLE_COLLECTION_CLASSES = IMMUTABLE_LIST_CLASSES + IMMUTABLE_SET_CLASSES

fun Any.isMutableCollection(): Boolean {
    if (this !is Collection<*>) return false
    return !IMMUTABLE_COLLECTION_CLASSES.contains(this.javaClass)
}