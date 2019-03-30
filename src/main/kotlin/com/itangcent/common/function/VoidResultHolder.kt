package com.itangcent.common.function

import java.util.concurrent.TimeUnit

class VoidResultHolder : AbstractResultHolder() {

    fun waitComplete() {
        resultLock.lock()
        try {
            if (running) {
                completed.await()
            }
            if (throwable != null)
                throw RuntimeException(throwable)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            resultLock.unlock()
        }
    }

    fun waitComplete(expiration: Long) {
        resultLock.lock()
        try {
            if (running) {
                completed.await(expiration, TimeUnit.MILLISECONDS)
            }
            if (throwable != null)
                throw RuntimeException(throwable)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            resultLock.unlock()
        }
    }
}
