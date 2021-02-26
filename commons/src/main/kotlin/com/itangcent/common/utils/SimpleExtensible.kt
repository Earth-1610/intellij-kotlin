package com.itangcent.common.utils

import java.util.concurrent.ConcurrentHashMap

interface Extensible {

    fun hasExt(attr: String): Boolean

    fun hasAnyExt(vararg attr: String): Boolean

    @Suppress("UNCHECKED_CAST")
    fun <T> getExt(attr: String): T?

    fun setExt(attr: String, value: Any?)

    fun exts(): Map<String, Any?>?
}

abstract class AbstractExtensible : Extensible {

    protected abstract fun map(): MutableMap<String, Any?>?

    protected abstract fun initMap(): MutableMap<String, Any?>

    override fun hasExt(attr: String): Boolean {
        return map()?.containsKey(attr) ?: false
    }

    override fun hasAnyExt(vararg attr: String): Boolean {
        val map: MutableMap<String, Any?> = map() ?: return false
        return attr.any { map.containsKey(it) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getExt(attr: String): T? {
        return map()?.get(attr) as T?
    }

    override fun setExt(attr: String, value: Any?) {
        var map = map()
        if (map == null) {
            map = initMap()
        }
        map[attr] = value
    }

    //no copy,please don't modify this map.
    override fun exts(): Map<String, Any?>? {
        return map()
    }
}

open class SimpleExtensible : AbstractExtensible() {

    private var ext: LinkedHashMap<String, Any?>? = null

    override fun map(): HashMap<String, Any?>? {
        return ext
    }

    override fun initMap(): HashMap<String, Any?> {
        var map = this.ext
        if (map == null) {
            map = LinkedHashMap()
            this.ext = map
        }
        return map
    }
}

/**
 *  A [Extensible] implementation that supporting full concurrency of retrievals and
 * high expected concurrency for updates.
 */
open class ConcurrentExtensible : AbstractExtensible() {

    private var ext: ConcurrentHashMap<String, Any?>? = null

    override fun map(): ConcurrentHashMap<String, Any?>? {
        return ext
    }

    override fun initMap(): ConcurrentHashMap<String, Any?> {
        synchronized(this) {
            var map = this.ext
            if (map == null) {
                map = ConcurrentHashMap()
                this.ext = map
            }
            return map
        }
    }

}

fun <T> Extensible.cache(attr: String, value: () -> T?): T? {
    return this.getExt<T>(attr) ?: value().also { this.setExt(attr, it) }
}