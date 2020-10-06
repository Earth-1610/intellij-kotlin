package com.itangcent.test

import com.itangcent.common.concurrent.AQSCountLatch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.TimeUnit

class AQSCountLatchTest {

    @Test
    fun testAQSCountLatch() {
        val aqs = AQSCountLatch()
        for (index in 1..5) {
            aqs.up()
            Thread {
                try {
                    TimeUnit.MILLISECONDS.sleep(100L * index)
                } finally {
                    aqs.down()
                }
            }.start()
        }
        assertTimeout(Duration.ofMillis(10)) { aqs.waitFor(100) }
    }
}