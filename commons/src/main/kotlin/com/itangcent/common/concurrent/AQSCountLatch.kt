package com.itangcent.common.concurrent

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import kotlin.jvm.Throws

class AQSCountLatch : CountLatch {

    private class Sync : AbstractQueuedSynchronizer() {

        internal val isUp: Boolean
            get() = state == 0

        public override fun tryAcquireShared(acquires: Int): Int {
            return if (state == 0) 1 else -1
        }

        internal fun getCount(): Int {
            return state
        }

        public override fun tryReleaseShared(releases: Int): Boolean {
            // Decrement count; signal when transition to zero
            while (true) {
                val c = state
                if (c == 0) return false
                val next = c - 1
                if (compareAndSetState(c, next)) return next == 0
            }
        }

        internal fun down() {
            while (true) {
                val current = state
                val next = current + 1
                if (compareAndSetState(current, next)) return
            }
        }
    }

    private val sync = Sync()

    override fun up() {
        tryUp()
    }

    fun tryUp(): Boolean {
        return sync.releaseShared(1)
    }

    override fun down() {
        sync.down()
    }

    override fun waitFor() {
        try {
            waitForUnsafe()
        } catch (e: InterruptedException) {
            throw com.itangcent.common.exception.ProcessCanceledException(e)
        }

    }

    @Throws(InterruptedException::class)
    fun waitForUnsafe() {
        sync.acquireSharedInterruptibly(1)
    }

    // true if semaphore became free
    override fun waitFor(msTimeout: Long): Boolean {
        try {
            return waitForUnsafe(msTimeout)
        } catch (e: InterruptedException) {
            throw com.itangcent.common.exception.ProcessCanceledException(e)
        }

    }

    // true if semaphore became free
    @Throws(InterruptedException::class)
    fun waitForUnsafe(msTimeout: Long): Boolean {
        return sync.tryAcquireSharedNanos(1, TimeUnit.MILLISECONDS.toNanos(msTimeout))
    }

    fun isUp(): Boolean {
        return sync.isUp
    }

    override fun count(): Int {
        return sync.getCount()
    }
}