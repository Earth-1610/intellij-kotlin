package com.itangcent.common.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * An interface for objects that can have extensions added to them.
 */
interface Extensible {

    /**
     * Checks if the given attribute exists in the extensions.
     *
     * @param attr The attribute to check.
     * @return `true` if the attribute exists, `false` otherwise.
     */
    fun hasExt(attr: String): Boolean

    /**
     * Checks if any of the given attributes exist in the extensions.
     *
     * @param attr The attributes to check.
     * @return `true` if any of the attributes exist, `false` otherwise.
     */
    fun hasAnyExt(vararg attr: String): Boolean

    /**
     * Gets the value of the extension attribute with the given name.
     *
     * @param attr The name of the extension attribute.
     * @return The value of the extension attribute, or `null` if it doesn't exist.
     */
    fun <T> getExt(attr: String): T?

    /**
     * Sets the value of the extension attribute with the given name.
     *
     * @param attr The name of the extension attribute.
     * @param value The value to set.
     */
    fun setExt(attr: String, value: Any?)

    /**
     * Gets all the extensions as a map of attribute names to values.
     *
     * @return A map of attribute names to values.
     */
    fun exts(): Map<String, Any?>?
}

/**
 * An abstract implementation of the [Extensible] interface.
 */
abstract class AbstractExtensible : Extensible {

    /**
     * Gets the map of extension attributes.
     */
    protected abstract fun map(): MutableMap<String, Any?>?

    /**
     * Initializes the map of extension attributes.
     */
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
        return map()?.get(attr) as? T
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

/**
 * A [Extensible] implementation that is backed by a [HashMap].
 */
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

/**
 * Caches the value of the extension attribute with the given name.
 *
 * This function checks if the extension attribute with the given name exists.
 * If it exists, it returns its value. If it does not exist, it evaluates the
 * provided `value` lambda function to compute the value, sets it as the
 * extension attribute value, and returns it.
 *
 * @param attr The name of the extension attribute.
 * @param value The lambda function to compute the value if it does not exist.
 * @return The value of the extension attribute, either from cache or computed.
 */
fun <T> Extensible.cache(attr: String, value: () -> T?): T? {
    return getExt<T>(attr) ?: value().also { setExt(attr, it) }
}