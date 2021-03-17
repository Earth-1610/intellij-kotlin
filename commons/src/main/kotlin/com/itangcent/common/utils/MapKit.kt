package com.itangcent.common.utils

fun <K, V> MutableMap<K, V>.safeComputeIfAbsent(key: K, mappingFunction: (K) -> V?): V? {
    var mappingValue: V? = null
    try {
        if (this.containsKey(key)) {
            return this[key]
        }
        mappingValue = mappingFunction(key)?.also { this[key] = it }
        mappingValue?.let { this.put(key, it) }
    } catch (e: NullPointerException) {
        return null
    } catch (e: ConcurrentModificationException) {
        mappingValue?.let { this[key] = it }
    }
    return mappingValue
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

fun Map<Any?, Any?>.flat(consumer: (String, String) -> Unit) {
    this.forEach { (key, value) ->
        if (key == null) return@forEach
        if (key is String) {
            flat(key, value, consumer)
        } else {
            flat(GsonUtils.toJson(key), value, consumer)
        }
    }
}

private fun flat(path: String, value: Any?, consumer: (String, String) -> Unit) {
    when (value) {
        null -> return
        is Collection<*> -> value.forEach { flat(path, it, consumer) }
        is Array<*> -> value.forEach { flat(path, it, consumer) }
        is Map<*, *> -> {
            value.forEach { (k, v) ->
                flat("$path.$k", v, consumer)
            }
        }
        is String -> consumer(path, value)
        else -> consumer(path, GsonUtils.toJson(value))
    }
}

fun Map<Any?, Any?>.flat(): Map<String, String> {
    val result: MutableMap<String, String> = LinkedHashMap()
    this.flat { key, value -> result[key] = value }
    return result
}

fun Map<Any?, Any?>.flatMulti(): MultiValuesMap<String, String> {
    val result: MultiValuesMap<String, String> = MultiValuesMap()
    this.flat { key, value -> result.put(key, value) }
    return result
}
