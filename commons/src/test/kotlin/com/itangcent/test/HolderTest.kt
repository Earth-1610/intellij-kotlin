package com.itangcent.test

import com.itangcent.common.concurrent.Holder
import com.itangcent.common.concurrent.MutableHolder
import com.itangcent.common.concurrent.ValueHolder
import com.itangcent.common.concurrent.VoidHolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.TimeUnit

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
    }

    @Test
    fun testValueHolder() {
        val holder = ValueHolder<String>()
        val holder2 = ValueHolder<Int>()
        Thread {
            holder.success("x")
            TimeUnit.MILLISECONDS.sleep(100)
            holder2.compute { 99 }
        }.start()
        assertEquals(holder.value(), "x")
        assertTimeout(Duration.ofMillis(200)) { holder2.value() }
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
}