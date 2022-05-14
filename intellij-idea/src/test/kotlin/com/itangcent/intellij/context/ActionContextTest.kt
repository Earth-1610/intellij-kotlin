package com.itangcent.intellij.context

import com.itangcent.common.concurrent.VoidHolder
import com.itangcent.common.exception.ProcessCanceledException
import com.itangcent.intellij.constant.EventKey
import com.itangcent.testFramework.ContextLightCodeInsightFixtureTestCase
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * Test case of [ActionContext]
 */
class ActionContextTest : ContextLightCodeInsightFixtureTestCase() {

    fun testCreateBoundary() {
        val boundary = actionContext.createBoundary()
        val cnt1 = AtomicInteger()
        val cnt2 = AtomicInteger()
        actionContext.runAsync {
            val boundary1 = actionContext.createBoundary()
            for (i in 1..20) {
                actionContext.runAsync {
                    Thread.sleep(i * 200L)
                    cnt1.getAndIncrement()
                }
            }
            boundary1.waitComplete()
            assertEquals(0, boundary1.count())
            assertEquals(20, cnt1.get())
        }
        actionContext.runAsync {
            val boundary2 = actionContext.createBoundary()
            for (i in 1..10) {
                actionContext.runAsync {
                    Thread.sleep(i * 200L)
                    cnt2.getAndIncrement()
                }
            }
            boundary2.waitComplete()
            assertEquals(0, boundary2.count())
            assertEquals(10, cnt2.get())
        }
        boundary.waitComplete()
        assertEquals(0, boundary.count())
        assertEquals(30, cnt1.get() + cnt2.get())
    }

    fun testCloseBoundary() {
        val boundary = actionContext.createBoundary()
        val cnt1 = AtomicInteger()
        actionContext.runAsync {
            val boundary1 = actionContext.createBoundary()
            for (i in 1..20) {
                actionContext.runAsync {
                    Thread.sleep(i * 200L)
                    cnt1.getAndIncrement()
                }
            }
            boundary1.waitComplete()
            assertEquals(0, boundary1.count())
            assertEquals(20, cnt1.get())
        }
        Thread.sleep(500)
        boundary.close()
        assertThrows<ProcessCanceledException> {
            actionContext.runAsync {
                fail()
            }
        }
        boundary.waitComplete()
        assertTrue(30 > cnt1.get())


        val cnt2 = AtomicInteger()
        val newBoundary = actionContext.createBoundary()
        actionContext.runAsync {
            val boundary1 = actionContext.createBoundary()
            for (i in 1..20) {
                actionContext.runAsync {
                    Thread.sleep(i * 200L)
                    cnt2.getAndIncrement()
                }
            }
            boundary1.waitComplete()
            assertEquals(0, boundary1.count())
            assertEquals(20, cnt2.get())
        }
        newBoundary.waitComplete()
        assertEquals(0, newBoundary.count())
        assertEquals(20, cnt2.get())

        actionContext.waitComplete()
    }

    fun testWaitComplete() {
        val cnt = AtomicInteger()
        for (i in 1..20) {
            actionContext.runAsync {
                Thread.sleep(i * 200L)
                cnt.getAndIncrement()
            }
        }
        actionContext.waitComplete()
        assertEquals(20, cnt.get())
    }

    fun testWaitCompleteAsync() {
        val cnt = AtomicInteger()
        val valueHolder = VoidHolder()
        actionContext.on(EventKey.ON_COMPLETED) {
            cnt.getAndIncrement()
            assertEquals(21, cnt.get())
            valueHolder.success()
        }
        for (i in 1..20) {
            actionContext.runAsync {
                Thread.sleep(i * 200L)
                cnt.getAndIncrement()
            }
        }
        actionContext.waitCompleteAsync()
        assertTrue(20 > cnt.get())
        valueHolder.value()
        assertEquals(21, cnt.get())
    }

    fun testHold() {
        val cnt = AtomicInteger()

        actionContext.hold()
        Thread {
            try {
                Thread.sleep(200)
                cnt.getAndIncrement()
            } finally {
                actionContext.unHold()
            }
        }.start()

        actionContext.hold()
        Thread {
            actionContext.runAsync {
                try {
                    Thread.sleep(200)
                    cnt.getAndIncrement()
                } finally {
                    actionContext.unHold()
                }
            }
        }.start()

        actionContext.waitComplete()
        assertEquals(2, cnt.get())
    }

    fun testRunInSwing() {
        var max = 0
        val cnt = AtomicInteger()
        for (i in 0..3) {
            actionContext.runInSwingUI {
                cnt.getAndIncrement()
                try {
                    max = max(max, cnt.get())
                    Thread.sleep(2000)
                } finally {
                    cnt.getAndDecrement()
                }
            }
        }
        actionContext.waitComplete()
        assertEquals(1, max)
    }

    fun testRunInWrite() {
        var max = 0
        val cnt = AtomicInteger()
        for (i in 0..3) {
            actionContext.runInWriteUI {
                cnt.getAndIncrement()
                try {
                    max = max(max, cnt.get())
                    Thread.sleep(2000)
                } finally {
                    cnt.getAndDecrement()
                }
            }
        }
        actionContext.waitComplete()
        assertEquals(1, max)
    }

    fun testRunInRead() {
        var max = 0
        val cnt = AtomicInteger()
        for (i in 0..3) {
            actionContext.runInReadUI {
                cnt.getAndIncrement()
                try {
                    max = max(max, cnt.get())
                    Thread.sleep(2000)
                } finally {
                    cnt.getAndDecrement()
                }
            }
        }
        actionContext.waitComplete()
        assertEquals(1, max)
    }

}