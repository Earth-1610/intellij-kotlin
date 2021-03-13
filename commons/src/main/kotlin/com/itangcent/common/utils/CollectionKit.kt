package com.itangcent.common.utils

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

fun Map<*, *>?.notNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

fun <K, V> Map<K, V>.asHashMap(): HashMap<K, V> {
    if (this is HashMap<K, V>) {
        return this
    }
    val map: HashMap<K, V> = HashMap()
    this.entries.forEach { map[it.key] = it.value }
    return map
}

fun <E> List<E>.asArrayList(): ArrayList<E> {
    if (this is ArrayList<E>) {
        return this
    }
    val list: ArrayList<E> = ArrayList()
    this.forEach { list.add(it) }
    return list
}