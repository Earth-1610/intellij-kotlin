package com.itangcent.intellij.extend.rx

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * A helper class for throttling requests
 */
@Deprecated("Directly use [Throttle] instead")
class ThrottleHelper {

    private val throttleMap = ConcurrentHashMap<Any, Throttle>()

    /**
     * get throttle by key
     */
    private fun getThrottle(key: Any) = throttleMap.computeIfAbsent(key) { Throttle() }

    /**
     * Acquires the throttle for the specified key with the given cooldown duration
     */
    fun acquire(key: Any, cd: Long): Boolean {
        return getThrottle(key).acquire(cd)
    }

    /**
     * Tries to force update the last request time to the current time for the specified key
     */
    fun refresh(key: Any): Boolean {
        return getThrottle(key).refresh()
    }

    /**
     * Tries to force update the last request time to the provided stamp for the specified key
     */
    fun refresh(key: Any, stamp: Long): Boolean {
        return getThrottle(key).refresh(stamp)
    }

    /**
     * Retrieves or creates a Throttle instance for the specified key
     */
    fun build(key: Any): Throttle = getThrottle(key)
}

/**
 * A throttle that limits the rate of requests
 */
class Throttle {
    private val lastRequestTime = AtomicLong(0)

    /**
     * Acquires the throttle with the given cooldown duration
     */
    fun acquire(cd: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val previousTime = lastRequestTime.get()

        if (currentTime < previousTime + cd) {
            return false
        }
        return lastRequestTime.compareAndSet(previousTime, currentTime)
    }

    /**
     * Updates the last request time to the current time
     */
    @Deprecated("will be removed in the future")
    fun refresh(): Boolean {
        val currentTime = System.currentTimeMillis()
        lastRequestTime.set(currentTime)
        return true
    }

    /**
     * Tries to force update the last request time to the provided stamp
     */
    @Deprecated("will be removed in the future")
    fun refresh(stamp: Long): Boolean {
        val previousTime = lastRequestTime.get()

        if (stamp <= previousTime) {
            return false
        }
        return lastRequestTime.compareAndSet(previousTime, stamp)
    }
}

fun throttle(): Throttle = Throttle()

@Deprecated("will be removed in the future")
fun (() -> Any?).throttle(timeout: Long, timeUnit: TimeUnit): () -> Unit {
    val timer = Timer()
    var task: TimerTask? = null
    val debounceTimeMillis = timeUnit.toMillis(timeout)
    var lastExecutionTime = 0L

    return {
        synchronized(this) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastExecution = currentTime - lastExecutionTime

            if (timeSinceLastExecution >= debounceTimeMillis) {
                // Execute the function immediately
                this.invoke()
                lastExecutionTime = currentTime
            } else {
                // Cancel any existing task
                task?.cancel()

                // Schedule the function for execution after the debounce time window
                task = object : TimerTask() {
                    override fun run() {
                        synchronized(this@throttle) {
                            this@throttle.invoke()
                            lastExecutionTime = System.currentTimeMillis()
                        }
                    }
                }
                timer.schedule(task, debounceTimeMillis - timeSinceLastExecution)
            }
        }
    }
}