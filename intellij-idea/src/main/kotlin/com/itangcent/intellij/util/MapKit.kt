package com.itangcent.intellij.util

fun <K, V> MutableMap<K, V>.computeIfAbsentSafely(key: K, mappingFunction: (K) -> V): V? {
    if (this.containsKey(key)) {
        return this[key]
    }

    synchronized(this) {
        if (this.containsKey(key)) {
            return this[key]
        }

        val value = mappingFunction(key)
        this[key] = value
        return value
    }

}