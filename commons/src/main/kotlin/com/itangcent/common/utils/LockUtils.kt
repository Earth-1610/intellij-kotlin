package com.itangcent.common.utils

import java.util.concurrent.Callable
import java.util.concurrent.locks.Lock


object LockUtils {

    fun runWithLock(lock: Lock, action: Runnable) {
        lock.lock()
        try {
            action.run()
        } finally {
            lock.unlock()
        }
    }

    fun tryRunWithLock(lock: Lock, action: Runnable): Boolean {
        return if (lock.tryLock()) {
            try {
                action.run()
            } finally {
                lock.unlock()
            }
            true
        } else {
            false
        }
    }

    fun <R> callWithLock(lock: Lock, callable: Callable<R>): R? {
        return callWithLock(lock, callable, null)
    }

    fun <R> callWithLock(lock: Lock, callable: Callable<R>, defaultValue: R?): R? {
        lock.lock()
        return try {
            callable.call()
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        } finally {
            lock.unlock()
        }
    }
}
