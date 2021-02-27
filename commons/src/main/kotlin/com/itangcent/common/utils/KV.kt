package com.itangcent.common.utils


import java.util.*

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
fun <T> Map<*, *>.getAs(key: Any?): T? {
    return this[key] as? T
}

@Suppress("UNCHECKED_CAST")
fun <T> Map<*, *>.getAs(key: Any?, subKey: Any?): T? {
    return this.getAs<Map<*, *>>(key)?.getAs(subKey)
}

@Suppress("UNCHECKED_CAST")
fun <T> Map<*, *>.getAs(key: Any?, subKey: Any?, grandKey: Any?): T? {
    return this.getAs<Map<*, *>>(key)?.getAs<Map<*, *>>(subKey)?.getAs(grandKey)
}

@Suppress("UNCHECKED_CAST")
fun KV<*, *>.getAsKv(key: String): KV<String, Any?>? {
    return this[key]?.asKV()
}

@Suppress("UNCHECKED_CAST")
fun KV<*, *>.sub(key: String): KV<String, Any?> {
    var subKV: KV<String, Any?>? = this[key] as KV<String, Any?>?
    if (subKV == null) {
        subKV = KV.create()
        (this as KV<String, Any?>)[key] = subKV
    }
    return subKV
}

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<out K, V>.mutable(copy: Boolean = false): MutableMap<K, V> {
    return when {
        copy -> LinkedHashMap(this)
        this is MutableMap -> this as MutableMap<K, V>
        else -> LinkedHashMap(this)
    }
}

@Suppress("UNCHECKED_CAST")
fun Any?.asKV(): KV<String, Any?> {
    if (this == null) {
        return KV.create()
    }
    if (this is KV<*, *>) {
        return this as KV<String, Any?>
    }
    if (this is Map<*, *>) {
        return KV<String, Any?>().set(this as Map<String, Any?>)
    }
    return KV.create()
}