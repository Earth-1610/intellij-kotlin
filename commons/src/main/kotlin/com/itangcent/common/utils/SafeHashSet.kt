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
