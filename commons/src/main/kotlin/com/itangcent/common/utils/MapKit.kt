package com.itangcent.common.utils

fun <K, V> MutableMap<K, V>.safeComputeIfAbsent(key: K, mappingFunction: (K) -> V): V? {
    var mappingValue: V? = null
    try {
        return this.computeIfAbsent(key) {
            mappingValue = mappingFunction(it)
            return@computeIfAbsent mappingValue!!
        }
    } catch (e: ConcurrentModificationException) {
        mappingValue?.let { this[key] = it }
        return mappingValue
    }
}