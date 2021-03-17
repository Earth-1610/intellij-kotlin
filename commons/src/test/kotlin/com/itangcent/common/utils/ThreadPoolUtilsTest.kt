package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThreadPoolUtilsTest {

    @Test
    fun testSetPoolSize() {
        val executorService: ExecutorService = Executors.newFixedThreadPool(2)
        assertTrue(executorService is ThreadPoolExecutor)
        assertEquals(2, executorService.maximumPoolSize)
        assertEquals(2, executorService.corePoolSize)
        ThreadPoolUtils.setPoolSize(executorService, 3)
        assertEquals(3, executorService.maximumPoolSize)
        assertEquals(3, executorService.corePoolSize)
    }

    @Test
    fun testSingle() {
        testSingle(ThreadPoolUtils.createSinglePool("ThreadPoolUtilsTest"))
        testSingle(ThreadPoolUtils.createSinglePool(ThreadPoolUtilsTest::class.java))
    }

    private fun testSingle(pool: ExecutorService) {
        val cnt = AtomicInteger()
        for (i in 0..2) {
            pool.submit {
                cnt.incrementAndGet()
                assertEquals(1, cnt.get())
                Thread.sleep(1000)
                cnt.decrementAndGet()
            }
        }
    }

    @Test
    fun testPool() {
        testPool(ThreadPoolUtils.createPool(2, 4, "ThreadPoolUtilsTest"))
        testPool(ThreadPoolUtils.createPool(2, 4, ThreadPoolUtilsTest::class.java))
    }

    private fun testPool(pool: ExecutorService) {
        val cnt = AtomicInteger()
        for (i in 0..4) {
            pool.submit {
                if (cnt.incrementAndGet() > 2) {
                    assertTrue((pool as ThreadPoolExecutor).poolSize > 2)
                }
                Thread.sleep(1000)
            }
        }
    }
}