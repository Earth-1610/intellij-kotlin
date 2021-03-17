package com.itangcent.common.concurrent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertNull

/**
 * Test case of [Holder]
 */
class HolderTest {

    @Test
    fun testDefaultHolder() {
        val holder = Holder.of("x")
        assertEquals(holder.value(), "x")
    }

    @Test
    fun testEmptyHolder() {
        val holder = Holder.nil<Any>()
        assertEquals(holder.value(), null)
    }

    @Test
    fun testMutableHolder() {
        val holder = MutableHolder.of("x")
        assertEquals(holder.value(), "x")
        holder.updateData("y")
        assertEquals(holder.value(), "y")
        holder.updateData { "${it}z" }
        assertEquals(holder.value(), "yz")
        holder.clear()
        assertNull(holder.value())
        val nilHolder = MutableHolder.nil<String>()
        assertNull(nilHolder.value())
        nilHolder.updateData("y")
        assertEquals(nilHolder.value(), "y")
        nilHolder.updateData { "${it}z" }
        assertEquals(nilHolder.value(), "yz")
        nilHolder.clear()
        assertNull(nilHolder.value())
    }

    @Test
    fun testValueHolder() {
        val holder = ValueHolder<String>()
        val holder2 = ValueHolder<Int>()
        assertTimeout(Duration.ofMillis(1000)) { holder.peek() }
        Thread {
            holder.success("x")
            TimeUnit.MILLISECONDS.sleep(100)
            holder2.compute { 99 }
        }.start()
        assertEquals(holder.value(), "x")
        assertEquals(holder.peek(), "x")
        assertTimeout(Duration.ofMillis(1000)) { holder2.value() }
        assertEquals(holder2.value(), 99)
    }

    @Test
    fun testValueHolderFailed() {
        val holder = ValueHolder<String>()
        val holder2 = ValueHolder<Int>()
        Thread {
            holder.failed("failed")
            holder2.compute {
                throw IllegalArgumentException("failed")
            }
        }.start()
        assertThrows(RuntimeException::class.java) {
            holder.value()
        }
        assertThrows(IllegalArgumentException::class.java) {
            holder2.value()
        }
    }

    @Test
    fun testVoidHolder() {
        val holder = VoidHolder()
        Thread {
            holder.success()
        }.start()
        assertEquals(holder.value(), null)
    }

    @Test
    fun testVoidHolderFailed() {
        val holder = VoidHolder()
        val holder2 = VoidHolder()
        val holder3 = VoidHolder()
        Thread {
            holder.failed("failed")
            holder2.compute {
                throw IllegalArgumentException("failed")
            }
            holder3.compute { }
        }.start()
        assertThrows(RuntimeException::class.java) {
            holder.value()
        }
        assertThrows(IllegalArgumentException::class.java) {
            holder2.value()
        }
        assertDoesNotThrow { holder3.value() }
    }
}