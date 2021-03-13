package com.itangcent.test

import com.itangcent.common.concurrent.AQSCountLatch
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test case for [AQSCountLatch]
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
    fun testWaitForCompleted() {
        assertTimeout(Duration.ofMillis(2000)) {
            assertTrue(aqs!!.waitFor(2000))
        }
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