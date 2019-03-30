package com.itangcent.common.function

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

open class AbstractResultHolder : Runnable {
    protected var throwable: Throwable? = null
    protected var resultLock: Lock = ReentrantLock()
    protected var completed = resultLock.newCondition()!!
    protected var running = true

    fun error(throwable: Throwable) {
        if (running) {
            resultLock.lock()
            try {
                this.throwable = throwable
                running = false
                completed.signalAll()
            } finally {
                resultLock.unlock()
            }
        }
    }

    fun complete() {
        if (running) {
            resultLock.lock()
            try {
                running = false
                completed.signalAll()
            } finally {
                resultLock.unlock()
            }
        }
    }

    override fun run() {
        complete()
    }
}
