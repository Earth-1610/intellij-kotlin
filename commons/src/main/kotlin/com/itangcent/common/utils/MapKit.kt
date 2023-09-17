package com.itangcent.common.utils

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.ILoggerProvider
import com.itangcent.common.spi.SpiUtils

object MapKit

fun <K, V> MutableMap<K, V>.safeComputeIfAbsent(key: K, mappingFunction: (K) -> V?): V? {
    var mappingValue: V? = null
    try {
        if (this.containsKey(key)) {
            return this[key]
        }
        computeIfAbsent(key) {
            mappingValue = mappingFunction(key)
            mappingValue!!
        }
    } catch (e: NullPointerException) {
        return null
    } catch (e: Exception) {
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


inline fun <reified T> Map<*, *>.getAs(key: Any?): T? {
    return this[key] as? T
}

inline fun <reified T> Map<*, *>.getAs(key: Any?, subKey: Any?): T? {
    return this.getAs<Map<*, *>>(key)?.getAs(subKey)
}

inline fun <reified T> Map<*, *>.getAs(key: Any?, subKey: Any?, grandKey: Any?): T? {
    return this.getAs<Map<*, *>>(key)?.getAs<Map<*, *>>(subKey)?.getAs(grandKey)
}

@Suppress("UNCHECKED_CAST")
fun Map<*, *>.sub(key: String): MutableMap<String, Any?> {
    return this[key].asMutableMap {
        (this as? MutableMap<String, Any?>)?.set(key, it)
    }
}

@Suppress("UNCHECKED_CAST")
fun Map<*, *>.getSub(key: String): Map<String, Any?>? {
    return this[key] as?  Map<String, Any?>
}

fun MutableMap<*, *>.merge(map: Map<*, *>): MutableMap<*, *> {
    map.forEach { this.merge(it.key, it.value) }
    return this
}

@Suppress("UNCHECKED_CAST")
fun MutableMap<*, *>.merge(key: Any?, value: Any?): MutableMap<*, *> {
    if (!this.containsKey(key)) {
        (this as MutableMap<Any?, Any?>)[key] = value
        return this
    }
    if (value.isOriginal()) {
        return this
    }
    val oldValue = this[key]
    if (oldValue.isOriginal()) {
        (this as MutableMap<Any?, Any?>)[key] = value
        return this
    }

    if (oldValue is Map<*, *> && value is Map<*, *>) {
        when {
            oldValue.isMutableMap() -> {
                (oldValue as MutableMap<Any?, Any?>).merge(value as Map<Any?, Any?>)
            }

            value.isMutableMap() -> {
                (value as MutableMap<Any?, Any?>).merge(oldValue as Map<Any?, Any?>)
                (this as MutableMap<Any?, Any?>)[key] = value
            }

            else -> {
                val mergeMap = LinkedHashMap<Any?, Any?>()
                mergeMap.putAll(oldValue as Map<Any?, Any?>)
                mergeMap.merge(value as Map<Any?, Any?>)
                (this as MutableMap<Any?, Any?>)[key] = mergeMap
            }
        }
        return this
    }

    if (oldValue is Collection<*> && value is Collection<*>) {
        when {
            oldValue.isMutableCollection() -> {
                try {
                    (oldValue as MutableCollection<Any?>).merge(value as Collection<Any?>)
                    return this
                } catch (e: UnsupportedOperationException) {
                    //ignore
                }
            }

            value.isMutableCollection() -> {
                try {
                    (value as MutableCollection<Any?>).merge(oldValue as Collection<Any?>)
                    (this as MutableMap<Any?, Any?>)[key] = value
                    return this
                } catch (e: UnsupportedOperationException) {
                    //ignore
                }
            }
        }
        val mergeCollection = if (oldValue is Set<*> || value is Set<*>) {
            HashSet()
        } else {
            ArrayList<Any?>()
        }
        mergeCollection.merge(oldValue as Collection<Any?>)
        mergeCollection.merge(value as Collection<Any?>)
        (this as MutableMap<Any?, Any?>)[key] = mergeCollection
        return this
    }

    if ((oldValue == null && value != null) ||
        (oldValue.isOriginal() && !value.isOriginal())
    ) {
        (this as MutableMap<Any?, Any?>).let {
            it[key] = value
        }
    }
    return this
}

@Suppress("UNCHECKED_CAST")
fun MutableCollection<*>.merge(collection: Collection<*>): MutableCollection<*> {
    this as MutableCollection<Any?>
    for (ele in collection) {
        if (ele.isOriginal() || this.contains(ele)) {
            continue
        }
        this.add(ele)
    }
    return this
}

/**
 * check if the object is original
 * like:
 * default primary: 0, 0.0
 * default blank string: ""
 * array with original: [0],[0.0],[""]
 * list with original: [0],[0.0],[""]
 * map with original: {"key":0}
 */
fun Any?.isOriginal(): Boolean {
    when (val obj = this) {
        null -> {
            return true
        }

        is Array<*> -> {
            return obj.size == 0 || (obj.size == 1 && obj[0].isOriginal())
        }

        is Collection<*> -> {
            return obj.size == 0 || (obj.size == 1 && obj.first().isOriginal())
        }

        is Map<*, *> -> {
            return obj.size == 0 || (obj.size == 1 && obj.entries.first().let {
                (it.key == "key" || it.key.isOriginal()) && it.value.isOriginal()
            })
        }

        is Boolean -> {
            return obj
        }

        is Number -> {
            return obj.toDouble() == 0.0
        }

        is String -> {
            return obj.isBlank()
        }

        else -> return false
    }
}

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<out K, V>.mutable(copy: Boolean = false): MutableMap<K, V> {
    if (!copy && this.isMutableMap()) {
        return this as MutableMap<K, V>
    }
    return LinkedHashMap(this)
}

@Suppress("UNCHECKED_CAST")
fun Any?.asMap(onCastFailed: (Map<String, Any?>) -> Unit = {}): Map<String, Any?> {
    if (this == null) {
        val ret = linkedMapOf<String, Any?>()
        onCastFailed(ret)
        return ret
    }
    if (this is Map<*, *>) {
        return this as Map<String, Any?>
    }
    LOG?.warn("can not cast ${GsonUtils.toJson(this)} as Map")
    val ret = linkedMapOf<String, Any?>()
    onCastFailed(ret)
    return ret
}

@Suppress("UNCHECKED_CAST")
fun Any?.asMutableMap(onCastFailed: (Map<String, Any?>) -> Unit = {}): MutableMap<String, Any?> {
    if (this == null) {
        val ret = linkedMapOf<String, Any?>()
        onCastFailed(ret)
        return ret
    }
    if (this is Map<*, *>) {
        return this.mutable() as MutableMap<String, Any?>
    }
    LOG?.warn("can not cast ${GsonUtils.toJson(this)} as Map")
    val ret = linkedMapOf<String, Any?>()
    onCastFailed(ret)
    return ret
}

@Suppress("UNCHECKED_CAST")
fun Map<*, *>.trySet(key: Any?, value: Any?) {
    try {
        (this as? MutableMap<Any?, Any?>)?.set(key, value)
    } catch (_: Exception) {
    }
}

fun Map<*, *>?.notNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

fun <K, V> Map<K, V>.asHashMap(): HashMap<K, V> {
    if (this is HashMap<K, V>) {
        return this
    }
    return linkedMapOf<K, V>().apply {
        putAll(this@asHashMap)
    }
}

private val immutableMapClasses = listOf(
    emptyMap<Any, Any>()::class,
    mapOf(1 to 1)::class
)

fun Any.isMutableMap(): Boolean {
    if (this !is MutableMap<*, *>) {
        return false
    }
    return !immutableMapClasses.contains(this::class)
}

private val LOG: ILogger? = SpiUtils.loadService(ILoggerProvider::class)?.getLogger(MapKit::class)