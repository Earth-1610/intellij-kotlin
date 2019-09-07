package com.itangcent.common.function

import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

class VoidResultHolder : AbstractResultHolder() {

    fun waitComplete() {
        resultLock.withLock {
            try {
                if (running) {
                    completed.await()
                }
                if (throwable != null) {
                    throw RuntimeException(throwable)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    fun waitComplete(expiration: Long) {
        resultLock.withLock {
            try {
                if (running) {
                    completed.await(expiration, TimeUnit.MILLISECONDS)
                }
                if (throwable != null) {
                    throw RuntimeException(throwable)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
