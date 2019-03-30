package com.itangcent.common.utils

import java.util.concurrent.Callable
import java.util.concurrent.locks.Lock


object LockUtils {

    /**
     * 使用锁同步执行代码
     *
     * @param lock   -同步锁
     * @param action -执行操作
     */
    fun runWithLock(lock: Lock, action: Runnable) {
        lock.lock()
        try {
            action.run()
        } finally {
            lock.unlock()
        }
    }

    /**
     * 尝试使用锁同步执行代码
     *
     * @param lock   -同步锁
     * @param action -执行操作
     * @return `true` if the lock was acquired and
     * `false` otherwise
     */
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


    /**
     * 使用锁同步执行代码
     *
     * @param lock     -同步锁
     * @param callable -A task that returns a result
     * @param <R>      －the result type of method `call`
     * @return -the result of the task and `null` if the task failed
    </R> */
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
