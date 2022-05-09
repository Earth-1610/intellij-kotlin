package com.itangcent.common.concurrent

import com.itangcent.common.exception.ProcessCanceledException
import org.junit.jupiter.api.*
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test case of [AQSCountLatch]
 */
class AQSCountLatchTest {

    private var aqs: AQSCountLatch? = null
    private var threadPool: ExecutorService? = null

    @BeforeEach
    fun before() {
        aqs = AQSCountLatch()
        threadPool = Executors.newFixedThreadPool(5)

        for (index in 1..5) {
            aqs!!.down()
            threadPool!!.submit {
                try {
                    TimeUnit.MILLISECONDS.sleep(100L * index)
                } finally {
                    aqs!!.up()
                }
            }
        }
    }

    @AfterEach
    fun afterAll() {
        threadPool?.shutdown()
        threadPool = null
        aqs = null
    }

    @Test
    fun testWaitForCompletedWithTimeOutSuccess() {
        assertTimeout(Duration.ofMillis(2000)) {
            assertTrue(aqs!!.waitFor(2000))
        }
    }

    @Test
    fun testWaitForCompletedWithTimeOutFailed() {
        aqs!!.down()
        threadPool!!.submit {
            try {
                TimeUnit.MILLISECONDS.sleep(2000L)
            } finally {
                aqs!!.up()
            }
        }
        assertFalse(aqs!!.waitFor(100))
    }

    @Test
    fun testWaitForSkipped() {
        aqs!!.down()
        threadPool!!.submit {
            try {
                TimeUnit.MILLISECONDS.sleep(5000)
            } finally {
                aqs!!.up()
            }
        }
        assertFalse(aqs!!.isUp())
        assertTimeout(Duration.ofMillis(500)) {
            assertFalse(aqs!!.waitFor(100))
        }
    }

    @Test
    fun testWaitForInterrupted() {
        aqs!!.down()
        try {
            val thread = Thread {
                assertTimeout(Duration.ofMillis(1000)) {
                    assertThrows<ProcessCanceledException> { aqs!!.waitFor() }
                }
            }
            thread.start()
            Thread.sleep(200)
            thread.interrupt()
        } finally {
            aqs!!.up()
        }
    }

    @Test
    fun testWaitForTimeInterrupted() {
        aqs!!.down()
        try {
            val thread = Thread {
                assertTimeout(Duration.ofMillis(1000)) {
                    assertThrows<ProcessCanceledException> { aqs!!.waitFor(2000) }
                }
            }
            thread.start()
            Thread.sleep(200)
            thread.interrupt()
        } finally {
            aqs!!.up()
        }
    }

    @Test
    fun testCount() {
        aqs!!.waitFor()
        assertEquals(0, aqs!!.count())
        assertTrue(aqs!!.isUp())
        aqs!!.down()
        try {
            assertEquals(1, aqs!!.count())
        } finally {
            aqs!!.up()
        }

    }
}