package com.itangcent.common.utils

import java.util.*

open class SafeHashSet<E> : HashSet<E>() {

    protected val failedElements = LinkedList<E>()

    override fun add(element: E): Boolean {
        try {
            if (this.contains(element)) {
                return false
            }
            return super.add(element)
        } catch (e: Throwable) {
            return if (failedElements.contains(element)) {
                false
            } else {
                failedElements.add(element)
                true
            }
        }
    }
}

open class SafeHashMap<K, V> : HashMap<K, V>() {

    protected val failedElements = LinkedList<Pair<K, V>>()

    override fun get(key: K): V? {
        return try {
            super.get(key)
        } catch (e: Throwable) {
            try {
                failedElements.find { it.first == key }
            } catch (e: Throwable) {
                failedElements.find { it.first === key }
            }?.second
        }
    }

    override fun getOrDefault(key: K, defaultValue: V): V {
        return try {
            super.getOrDefault(key, defaultValue)
        } catch (e: Exception) {
            try {
                failedElements.find { it.first == key }
            } catch (e: Throwable) {
                failedElements.find { it.first === key }
            }?.second ?: defaultValue
        }
    }

    override fun put(key: K, value: V): V? {
        return try {
            super.put(key, value)
        } catch (e: Throwable) {
            var ret: V? = null
            val index: Int = try {
                failedElements.indexOfFirst { it.first == key }
            } catch (e: Throwable) {
                failedElements.indexOfFirst { it.first === key }
            }
            if (index != -1) {
                ret = failedElements.removeAt(index).second
            }
            failedElements.add(key to value)
            ret
        }
    }
}

class ReentrantSafeHashSet<E>(private val reentrant: Int) : SafeHashSet<CntElement<E>>() {

    fun addElement(element: E): Boolean {
        return add(CntElement(element))
    }

    override fun add(element: CntElement<E>): Boolean {
        if (super.add(element)) {
            return true
        }
        return (find { it == element } ?: failedElements.find { it == element })!!.incr() <= reentrant
    }
}

class CntElement<E>(val e: E) {
    /**
     * 1 be init
     */
    var cnt = 1

    fun incr(): Int {
        return ++cnt
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CntElement<*>

        if (e != other.e) return false

        return true
    }

    override fun hashCode(): Int {
        return e?.hashCode() ?: 0
    }

    override fun toString(): String {
        return e.toString()
    }

}
