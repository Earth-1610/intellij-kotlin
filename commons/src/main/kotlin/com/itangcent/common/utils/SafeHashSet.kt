package com.itangcent.common.utils

import java.util.*

class SafeHashSet<E> : HashSet<E>() {

    private val failedElements = LinkedList<E>()

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