package com.itangcent.common.utils

object KV {

    fun <K, V> of(key: K, value: V): LinkedHashMap<K, V> = linkedMapOf(key to value)

    @Deprecated(
        "use 'of' instead",
        ReplaceWith("of(key, value)", "com.itangcent.common.utils.KV.Companion.of")
    )
    fun <K, V> by(key: K, value: V) = of(key, value)

    fun <K, V> create() = linkedMapOf<K, V>()

    fun any() = linkedMapOf<Any?, Any?>()
}
