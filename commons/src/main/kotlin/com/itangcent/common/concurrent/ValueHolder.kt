package com.itangcent.common.concurrent

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ValueHolder<T> {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var error: Throwable? = null

    @Volatile
    private var data: Any? = INIT_DATA

    @Suppress("UNCHECKED_CAST")
    fun getData(): T? {

        if (error != null) {
            throw error!!
        }
        if (data == INIT_DATA) {
            lock.withLock {
                while (data == INIT_DATA) {
                    condition.await()
                }
            }
        }
        if (error != null) {
            throw error!!
        }
        return data as T?
    }

    fun compute(action: (() -> T?)) {
        try {
            this.success(action())
        } catch (e: Exception) {
            this.failed(e)
        }
    }

    fun success(data: T?) {
        lock.withLock {
            this.data = data
            condition.signalAll()
        }
    }

    fun failed(error: Throwable) {
        lock.withLock {
            this.error = error
            condition.signalAll()
        }
    }

    companion object {
        private val INIT_DATA: Any = Object()
    }
}
