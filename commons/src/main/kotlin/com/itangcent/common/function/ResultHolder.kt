package com.itangcent.common.function

import kotlin.concurrent.withLock

/**
 * Created by tangcent on 3/17/17.
 */
class ResultHolder<T> : AbstractResultHolder(), Runnable {
    private var resultVal: T? = null

    fun getResultVal(): T? {
        return resultLock.withLock {
            try {
                if (running) {
                    completed.await()
                }
                if (throwable != null)
                    throw RuntimeException(throwable)
                resultVal
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            }
        }
    }

    fun setResultVal(resultVal: T?) {
        resultLock.withLock {
            this.resultVal = resultVal
            running = false
            completed.signalAll()
        }
    }

    fun peekResult(): T? {
        return resultLock.withLock {
            this.resultVal
        }
    }
}
