package com.itangcent.common.threadpool.eager

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class EagerThreadPoolExecutorTest {

    /**
     * It print like this:
     * thread number in current pool：1,  task number in task queue：0 executor size: 1
     * thread number in current pool：2,  task number in task queue：0 executor size: 2
     * thread number in current pool：3,  task number in task queue：0 executor size: 3
     * thread number in current pool：4,  task number in task queue：0 executor size: 4
     * thread number in current pool：5,  task number in task queue：0 executor size: 5
     * thread number in current pool：6,  task number in task queue：0 executor size: 6
     * thread number in current pool：7,  task number in task queue：0 executor size: 7
     * thread number in current pool：8,  task number in task queue：0 executor size: 8
     * thread number in current pool：9,  task number in task queue：0 executor size: 9
     * thread number in current pool：10,  task number in task queue：0 executor size: 10
     * thread number in current pool：10,  task number in task queue：4 executor size: 10
     * thread number in current pool：10,  task number in task queue：3 executor size: 10
     * thread number in current pool：10,  task number in task queue：2 executor size: 10
     * thread number in current pool：10,  task number in task queue：1 executor size: 10
     * thread number in current pool：10,  task number in task queue：0 executor size: 10
     *
     *
     * We can see , when the core threads are in busy,
     * the thread pool create thread (but thread nums always less than max) instead of put task into queue.
     */
    @Test
    fun testEagerThreadPool() {
        val name = "eager"
        val cores = 5
        val threads = 10
        // alive 1 second
        val alive: Long = 1000

        //init executor
        val executor = EagerThreadPoolExecutor(
            cores, threads,
            alive, TimeUnit.MILLISECONDS,
            BasicThreadFactory.Builder().daemon(true)
                .namingPattern("$name-%d").build(),
            ThreadPoolExecutor.AbortPolicy()
        )
        for (i in 0..14) {
            Thread.sleep(50)
            executor.execute {
                println(
                    "thread number in current pool：${executor.poolSize},  " +
                            "task number in task queue：${executor.queue.size}" +
                            " executor size: ${executor.poolSize}"
                )
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        Thread.sleep(5000)
        // cores threads are all alive.
        Assertions.assertEquals(executor.poolSize, cores, "more than cores threads alive!")
    }

    @Test
    fun testShutDown() {

        val executor = EagerThreadPoolExecutor(
            2, 3,
            1000, TimeUnit.MILLISECONDS,
            10,
            BasicThreadFactory.Builder().daemon(true)
                .namingPattern("eager-%d").build(),
            ThreadPoolExecutor.AbortPolicy()
        )
        executor.shutdown()
        assertThrows<RejectedExecutionException> {
            executor.submit {
                Thread.sleep(1000)
            }
        }
    }

}