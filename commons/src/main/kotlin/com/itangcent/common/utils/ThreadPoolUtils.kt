package com.itangcent.common.utils

import com.itangcent.common.threadpool.eager.EagerThreadPoolExecutor
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadPoolUtils {

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
        return EagerThreadPoolExecutor(
            poolSize, poolSize,
            0L, TimeUnit.MILLISECONDS,
            BasicThreadFactory.Builder().daemon(true)
                .namingPattern("$name-%d").build(),
            ThreadPoolExecutor.AbortPolicy()
        )
    }

    fun createPool(poolSize: Int, maximumPoolSize: Int, clazz: Class<*>): ExecutorService {
        return createPool(poolSize, maximumPoolSize, clazz.simpleName)
    }

    fun createPool(poolSize: Int, maximumPoolSize: Int, name: String): ExecutorService {
        return EagerThreadPoolExecutor(
            poolSize, maximumPoolSize,
            0L, TimeUnit.MILLISECONDS,
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
}
