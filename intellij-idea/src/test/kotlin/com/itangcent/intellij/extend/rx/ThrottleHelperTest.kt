package com.itangcent.intellij.extend.rx

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test case of [ThrottleHelper]
 */
class ThrottleHelperTest {

    @Test
    fun `test throttle with key acquisition and refresh`() {
        val throttleHelper = ThrottleHelper()

        val key = "api_endpoint"
        val cd = 1000L

        // Acquire throttle and assert true
        val acquired1 = throttleHelper.acquire(key, cd)
        assertTrue(acquired1)

        // Try to acquire throttle again within the cooldown period and assert false
        val acquired2 = throttleHelper.acquire(key, cd)
        assertFalse(acquired2)

        // Wait for the cooldown period to expire
        Thread.sleep(cd)

        // Try to acquire throttle again after cooldown period has expired and assert true
        val acquired3 = throttleHelper.acquire(key, cd)
        assertTrue(acquired3)

        // Refresh the throttle and assert true
        val refreshed = throttleHelper.refresh(key)
        assertTrue(refreshed)

        // Try to acquire throttle again after refreshing and assert false
        val acquired4 = throttleHelper.acquire(key, cd)
        assertFalse(acquired4)

        // Refresh the throttle with a specific timestamp and assert true
        val stamp = System.currentTimeMillis() + 5000
        val refreshedStamp = throttleHelper.refresh(key, stamp)
        assertTrue(refreshedStamp)

        // Try to acquire throttle again after refreshing with a specific timestamp and assert false
        val acquired5 = throttleHelper.acquire(key, cd)
        assertFalse(acquired5)

        // Try to refresh the throttle with a specific timestamp that is less than the current time and assert false
        assertFalse(throttleHelper.refresh(key, System.currentTimeMillis() - 1000))
    }

    @Test
    fun `test throttle acquisition and refresh`() {
        val throttle = ThrottleHelper().build("api_endpoint")

        val cd = 1000L

        // Acquire throttle and assert true
        val acquired1 = throttle.acquire(cd)
        assertTrue(acquired1)

        // Try to acquire throttle again within the cooldown period and assert false
        val acquired2 = throttle.acquire(cd)
        assertFalse(acquired2)

        // Wait for the cooldown period to expire
        Thread.sleep(cd)

        // Try to acquire throttle again after cooldown period has expired and assert true
        val acquired3 = throttle.acquire(cd)
        assertTrue(acquired3)

        // Refresh the throttle and assert true
        val refreshed = throttle.refresh()
        assertTrue(refreshed)

        // Try to acquire throttle again after refreshing and assert false
        val acquired4 = throttle.acquire(cd)
        assertFalse(acquired4)

        // Refresh the throttle with a specific timestamp and assert true
        val stamp = System.currentTimeMillis() + 5000
        val refreshedStamp = throttle.refresh(stamp)
        assertTrue(refreshedStamp)

        // Try to acquire throttle again after refreshing with a specific timestamp and assert false
        val acquired5 = throttle.acquire(cd)
        assertFalse(acquired5)

        // Try to refresh the throttle with a specific timestamp that is less than the current time and assert false
        assertFalse(throttle.refresh(System.currentTimeMillis() - 1000))
    }
}