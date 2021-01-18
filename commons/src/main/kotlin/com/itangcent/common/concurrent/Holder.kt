package com.itangcent.common.concurrent

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface Holder<T> {

    fun value(): T?

    companion object {

        private val NULL_HOLDER = NullHolder<Any>()

        fun <T> of(data: T): Holder<T> {
            return DefaultHolder(data)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> nil(): Holder<T> {
            return NULL_HOLDER as Holder<T>
        }
    }
}

private class DefaultHolder<T>(private val holdData: T? = null) : Holder<T> {

    override fun value(): T? {
        return this.holdData
    }
}

private class NullHolder<T> : Holder<T> {

    override fun value(): T? {
        return null
    }
}

abstract class AbstractHolder<T> : Holder<T> {

    protected val lock = ReentrantLock()

    protected val condition: Condition = lock.newCondition()

    protected var error: Throwable? = null

    @Suppress("UNCHECKED_CAST")
    override fun value(): T? {

        if (!isComputed()) {
            lock.withLock {
                while (!isComputed()) {
                    condition.await()
                }
            }
        }

        if (error != null) {
            throw error!!
        }

        return data()
    }

    protected abstract fun isComputed(): Boolean

    protected abstract fun data(): T?

    fun failed(msg: String) {
        lock.withLock {
            this.error = RuntimeException(msg)
            condition.signalAll()
        }
    }

    fun failed(error: Throwable) {
        lock.withLock {
            this.error = error
            condition.signalAll()
        }
    }
}

class ValueHolder<T> : AbstractHolder<T>() {

    override fun isComputed(): Boolean {
        return error != null || coreData != INIT_DATA
    }

    @Suppress("UNCHECKED_CAST")
    override fun data(): T? {
        return coreData as T?
    }

    private var coreData: Any? = INIT_DATA

    fun compute(action: (() -> T?)) {
        try {
            this.success(action())
        } catch (e: Exception) {
            this.failed(e)
        }
    }

    fun success(data: T?) {
        lock.withLock {
            this.coreData = data
            condition.signalAll()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun peek(): T? {
        lock.withLock {
            if (coreData == INIT_DATA) {
                return null
            }
            return this.coreData as T?
        }
    }

    companion object {
        private val INIT_DATA: Any = Object()
    }
}

class VoidHolder : AbstractHolder<Void>() {
    override fun isComputed(): Boolean {
        return error != null || computed
    }

    @Suppress("UNCHECKED_CAST")
    override fun data(): Void? {
        return null
    }

    private var computed = false

    fun compute(action: (() -> Unit)) {
        try {
            action()
            this.success()
        } catch (e: Exception) {
            this.failed(e)
        }
    }

    fun success() {
        lock.withLock {
            this.computed = true
            condition.signalAll()
        }
    }
}

interface MutableHolder<T> : Holder<T> {

    fun updateData(data: T?)

    fun updateData(updater: (T?) -> T?): T?

    fun clear()

    companion object {
        fun <T> of(data: T): MutableHolder<T> {
            return DefaultMutableHolder(data)
        }

        fun <T> nil(): MutableHolder<T> {
            return DefaultMutableHolder(null)
        }
    }
}

private open class DefaultMutableHolder<T> @JvmOverloads constructor(data: T? = null) : MutableHolder<T> {

    private var holdData: T? = null

    init {
        this.holdData = data
    }

    override fun value(): T? {
        return this.holdData
    }

    override fun updateData(data: T?) {
        this.holdData = data
    }

    override fun updateData(updater: (T?) -> T?): T? {
        this.holdData = updater(holdData)
        return this.holdData
    }

    override fun clear() {
        this.holdData = null
    }
}