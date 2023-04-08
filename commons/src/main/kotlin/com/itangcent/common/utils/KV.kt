package com.itangcent.common.utils


import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.ILoggerProvider
import com.itangcent.common.spi.SpiUtils

open class KV<K, V> : LinkedHashMap<K, V>() {

    open operator fun set(key: K, value: V): KV<K, V> {
        super.put(key, value)
        return this
    }

    fun set(map: Map<K, V>): KV<K, V> {
        super.putAll(map)
        return this
    }

    fun set(KV: KV<K, V>): KV<K, V> {
        super.putAll(KV)
        return this
    }

    fun delete(key: K): KV<K, V> {
        super.remove(key)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getAs(key: K): T {
        return get(key) as T
    }

    override fun equals(other: Any?): Boolean {
        return other is KV<*, *> && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(1)
    }

    override fun clone(): KV<K, V> {
        return create<K, V>().set(this)
    }

    companion object {

        fun <K, V> by(key: K, value: V): KV<K, V> {
            val kv: KV<K, V> = KV()
            return kv.set(key, value)
        }

        fun <K, V> create(): KV<K, V> {
            return KV()
        }

        fun any(): KV<Any?, Any?> {
            return KV()
        }
    }
}

@Suppress("UNCHECKED_CAST")
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
fun Map<*, *>.sub(key: String): Map<String, Any?> {
    return this[key].asMap {
        (this as? MutableMap<String, Any?>)?.set(key, it)
    }
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
            oldValue is MutableCollection<*> -> {
                try {
                    (oldValue as MutableCollection<Any?>).merge(value as Collection<Any?>)
                    return this
                } catch (e: UnsupportedOperationException) {
                    //ignore
                }
            }

            value is MutableCollection<*> -> {
                try {
                    (value as MutableCollection<Any?>).merge(oldValue as Collection<Any?>)
                    (this as MutableMap<Any?, Any?>)[key] = value
                    return this
                } catch (e: UnsupportedOperationException) {
                    //ignore
                }
            }
        }
        val mergeMap = if (oldValue is Set<*> || value is Set<*>) {
            HashSet()
        } else {
            ArrayList<Any?>()
        }
        mergeMap.merge(oldValue as Collection<Any?>)
        mergeMap.merge(value as Collection<Any?>)
        (this as MutableMap<Any?, Any?>)[key] = mergeMap
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

fun KV<*, *>.getAsKv(key: String): KV<String, Any?>? {
    return this[key]?.asKV()
}

@Suppress("UNCHECKED_CAST")
fun KV<*, *>.sub(key: String): KV<String, Any?> {
    return this[key].asKV {
        (this as KV<String, Any?>)[key] = it
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
fun Any?.asKV(onCastFailed: (KV<String, Any?>) -> Unit = {}): KV<String, Any?> {
    if (this == null) {
        val ret = KV.create<String, Any?>()
        onCastFailed(ret)
        return ret
    }
    if (this is KV<*, *>) {
        return this as KV<String, Any?>
    }
    if (this is Map<*, *>) {
        return KV<String, Any?>().set(this as Map<String, Any?>)
    }
    LOG?.warn("can not cast ${GsonUtils.toJson(this)} as KV")
    val ret = KV.create<String, Any?>()
    onCastFailed(ret)
    return ret
}

@Suppress("UNCHECKED_CAST")
fun Any?.asMap(onCastFailed: (Map<String, Any?>) -> Unit = {}): Map<String, Any?> {
    if (this == null) {
        val ret = HashMap<String, Any?>()
        onCastFailed(ret)
        return ret
    }
    if (this is Map<*, *>) {
        return this as Map<String, Any?>
    }
    LOG?.warn("can not cast ${GsonUtils.toJson(this)} as KV")
    val ret = HashMap<String, Any?>()
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

private val LOG: ILogger? = SpiUtils.loadService(ILoggerProvider::class)?.getLogger(KV::class)