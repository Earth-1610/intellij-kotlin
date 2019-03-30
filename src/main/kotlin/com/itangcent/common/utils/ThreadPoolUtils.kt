package com.itangcent.common.utils

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
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
        return ThreadPoolExecutor(
            poolSize, poolSize,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            BasicThreadFactory.Builder().daemon(true)
                .namingPattern("$name-%d").build()
        )
    }

    fun createSinglePool(clazz: Class<*>): ExecutorService {
        return createPool(1, clazz)
    }

    fun createSinglePool(name: String): ExecutorService {
        return createPool(1, name)
    }
}
