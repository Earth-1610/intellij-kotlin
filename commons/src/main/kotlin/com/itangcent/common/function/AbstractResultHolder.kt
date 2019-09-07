package com.itangcent.common.function

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class AbstractResultHolder : Runnable {
    protected var throwable: Throwable? = null
    protected var resultLock: Lock = ReentrantLock()
    protected var completed = resultLock.newCondition()!!
    protected var running = true

    fun error(throwable: Throwable) {
        if (running) {
            resultLock.withLock {
                this.throwable = throwable
                running = false
                completed.signalAll()
            }
        }
    }

    fun complete() {
        if (running) {
            resultLock.withLock {
                running = false
                completed.signalAll()
            }
        }
    }

    override fun run() {
        complete()
    }
}
