package com.itangcent.common.utils

import com.itangcent.common.logger.Log
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import java.util.concurrent.*

/**
 * Helpers for [ExecutorService]
 */
object ThreadPoolUtils : Log() {

    fun setPoolSize(threadPool: ExecutorService, poolSize: Int?) {
        if (threadPool is ThreadPoolExecutor) {
            threadPool.maximumPoolSize = poolSize!!
            threadPool.corePoolSize = poolSize
        }
    }

    fun createPool(poolSize: Int, clazz: Class<*>): ExecutorService {
        return createPool(poolSize, clazz.simpleName)
    }

    fun createPool(poolSize: Int, name: String): ExecutorService {
        return createPool(poolSize, poolSize, name)
    }

    fun createPool(poolSize: Int, maximumPoolSize: Int, clazz: Class<*>): ExecutorService {
        return createPool(poolSize, maximumPoolSize, clazz.simpleName)
    }

    fun createPool(poolSize: Int, maximumPoolSize: Int, name: String): ExecutorService {
        return ThreadPoolExecutor(
            poolSize,
            maximumPoolSize,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            BasicThreadFactory.Builder().daemon(true)
                .namingPattern("$name-%d").build(),
            ThreadPoolExecutor.AbortPolicy()
        )
    }

    fun createSinglePool(clazz: Class<*>): ExecutorService {
        return createPool(1, clazz)
    }

    fun createSinglePool(name: String): ExecutorService {
        return createPool(1, name)
    }

    fun newCachedThreadPool(): ExecutorService {
        return ThreadPoolExecutor(
            0, Int.MAX_VALUE,
            20L, TimeUnit.SECONDS,
            SynchronousQueue()
        )
    }

    fun newCachedThreadPool(threadFactory: ThreadFactory): ExecutorService {
        return ThreadPoolExecutor(
            0, Int.MAX_VALUE,
            20L, TimeUnit.SECONDS,
            SynchronousQueue(), threadFactory
        )
    }
}
