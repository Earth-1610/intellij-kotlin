package com.itangcent.intellij.extend.rx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.ceil

class ThrottleHelperKtTest {

    @Test
    fun `throttled function should execute once per throttle time window`() {
        var executionCount = 0
        val throttledFunction = {
            executionCount++
        }.throttle(500, TimeUnit.MILLISECONDS)

        // Invoke the throttled function multiple times within the throttle time window
        for (i in 1..10) {
            Thread.sleep(100) // Sleep for 100 milliseconds between invocations
            throttledFunction()
        }

        // Sleep for an additional 600 milliseconds to ensure the throttle time window has elapsed
        Thread.sleep(600)

        // Invoke the throttled function again
        throttledFunction()

        // Assert that the function was executed only twice
        assertEquals(4, executionCount)
    }


    @Test
    fun `throttled function should execute once per throttle time window in multi-threaded environment`() {
        val executionCount = AtomicInteger(0)
        val throttleTimeMillis = 500L
        val throttledFunction = {
            executionCount.incrementAndGet()
        }.throttle(throttleTimeMillis, TimeUnit.MILLISECONDS)

        val start = System.currentTimeMillis()
        // Create multiple threads to concurrently invoke the throttled function
        val concurrencyLevel = 10
        val latch = CountDownLatch(concurrencyLevel)
        repeat(concurrencyLevel) {
            thread {
                repeat(3) {
                    throttledFunction()
                    Thread.sleep(100) // Sleep for 100 milliseconds between invocations
                }
                latch.countDown()
            }
        }

        // Wait for all threads to finish
        latch.await()

        // Sleep for an additional 600 milliseconds to ensure the throttle time window has elapsed
        Thread.sleep(throttleTimeMillis + 100)

        // Invoke the throttled function again
        throttledFunction()

        // Assert that the function was executed only once per throttle time window
        assertEquals(
            ceil((System.currentTimeMillis() - start).toDouble() / throttleTimeMillis).toInt(),
            executionCount.get()
        )
    }
}