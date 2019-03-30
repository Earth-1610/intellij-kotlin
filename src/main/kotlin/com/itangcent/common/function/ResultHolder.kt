package com.itangcent.common.function

/**
 * Created by tangcent on 3/17/17.
 */
class ResultHolder<T> : AbstractResultHolder(), Runnable {
    private var resultVal: T? = null

    fun getResultVal(): T? {
        resultLock.lock()
        try {
            if (running) {
                completed.await()
            }
            if (throwable != null)
                throw RuntimeException(throwable)
            return resultVal
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        } finally {
            resultLock.unlock()
        }
    }

    fun setResultVal(resultVal: T?) {
        resultLock.lock()
        try {
            this.resultVal = resultVal
            running = false
            completed.signalAll()
        } finally {
            resultLock.unlock()
        }
    }

    fun peekResult(): T? {
        resultLock.lock()
        try {
            return this.resultVal
        } finally {
            resultLock.unlock()
        }
    }
}
