package com.itangcent.common.utils

fun <K, V> MutableMap<K, V>.safeComputeIfAbsent(key: K, mappingFunction: (K) -> V?): V? {
    var mappingValue: V? = null
    try {
        return this.computeIfAbsent(key) {
            mappingValue = mappingFunction(it)
            return@computeIfAbsent mappingValue!!
        }
    } catch (e: NullPointerException) {
        return null
    } catch (e: java.lang.NullPointerException) {
        return null
    } catch (e: ConcurrentModificationException) {
        mappingValue?.let { this[key] = it }
        return mappingValue
    }
}


fun <K, V> Map<K, V>?.any(vararg ks: K): V? {
    if (this == null) return null
    for (k in ks) {
        val v = this[k]
        if (v != null) {
            return v
        }
    }
    return null
}

fun <K> MutableMap<K, String?>?.append(key: K, str: String?, split: String = " ") {
    if (this == null) return
    this[key] = this[key].append(str, split)
}