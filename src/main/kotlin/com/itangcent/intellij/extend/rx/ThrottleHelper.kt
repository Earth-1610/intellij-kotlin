package com.itangcent.intellij.extend.rx

import com.google.common.base.Supplier
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.util.concurrent.atomic.AtomicReference

class ThrottleHelper {

    private var weakCache: LoadingCache<Any, TimeStamp> = CacheBuilder
        .newBuilder()
        .weakKeys()
        .build<Any, TimeStamp>(CacheLoader.from(Supplier { TimeStamp() }))

    fun acquire(key: Any, cd: Long): Boolean {
        val now = System.currentTimeMillis()
        val maxPre = now - cd
        val timeStamp = weakCache.getUnchecked(key)
        for (i in 0..maxTry) {
            val timeAndIndex = timeStamp.timeAndIndex()
            if (timeAndIndex.first > maxPre) {
                return false
            }

            if (timeStamp.tryUpdate(timeAndIndex.second, now)) {
                return true
            }
        }
        return false
    }

    /**
     * try force update stamp to now
     */
    fun refresh(key: Any): Boolean {
        return refresh(key, System.currentTimeMillis())
    }

    /**
     * try force update stamp
     */
    fun refresh(key: Any, stamp: Long): Boolean {
        val timeStamp = weakCache.getUnchecked(key)
        for (i in 0..maxTry) {
            val timeAndIndex = timeStamp.timeAndIndex()
            if (timeStamp.tryUpdate(timeAndIndex.second, stamp)) {
                return true
            }
        }
        return false
    }

    fun build(key: Any): Throttle {
        return Throttle(this, key)
    }

    class TimeStamp {

        private var timeWithIndex: AtomicReference<String> = AtomicReference()

        constructor() {
            this.timeWithIndex.set("0${split}0")
        }

        fun tryUpdate(expectIndex: Int, stamp: Long): Boolean {

            val oldTimeAndIndexStr = timeWithIndex.get()
            val timeAndIndex = parseTimeAndIndex(oldTimeAndIndexStr)
            if (timeAndIndex.second != expectIndex) {
                return false
            }

            val nextIndex = expectIndex + 1
            return this.timeWithIndex.compareAndSet(
                oldTimeAndIndexStr,
                "$stamp$split$nextIndex"
            )
        }

        fun timeAndIndex(): Pair<Long, Int> {
            val timeAndIndex = timeWithIndex.get()
            return parseTimeAndIndex(timeAndIndex)
        }

        private fun parseTimeAndIndex(timeAndIndex: String): Pair<Long, Int> {
            val split = timeAndIndex.split(split)
            return split[0].toLong() to split[1].toInt()
        }

    }

    companion object {
        private const val split = ','
        private const val maxTry = 10
    }

}

class Throttle(private var throttleHelper: ThrottleHelper, private val key: Any) {
    fun acquire(cd: Long): Boolean {
        return throttleHelper.acquire(key, cd)
    }

    /**
     * try force update stamp to now
     */
    fun refresh(): Boolean {
        return throttleHelper.refresh(key)
    }

    /**
     * try force update stamp
     */
    fun refresh(stamp: Long): Boolean {
        return throttleHelper.refresh(key, stamp)
    }

}