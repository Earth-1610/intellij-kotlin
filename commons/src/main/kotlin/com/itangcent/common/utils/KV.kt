package com.itangcent.common.utils

object KV {

    fun <K, V> of(key: K, value: V): LinkedHashMap<K, V> = linkedMapOf(key to value)

    fun <K, V> create() = linkedMapOf<K, V>()

    fun any() = linkedMapOf<Any?, Any?>()
}
