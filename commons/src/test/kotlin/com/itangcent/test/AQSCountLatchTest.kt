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
            TimeUnit.MILLISECONDS.sleep(10)
            Thread {
                for (ignore in 0..index) {
                    try {
                        aqs.up()
                        TimeUnit.MILLISECONDS.sleep(10)
                    } finally {
                        aqs.down()
                    }
                }
            }.start()
        }
        assertTimeout(Duration.ofMillis(1000)) { aqs.waitFor(2000) }
    }
}