package com.itangcent.common.string

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Test case of [GracefulToString]
 */
internal class GracefulToStringKtTest {

    @Test
    fun gracefulString() {
        assertNull(null.gracefulString())
        assertEquals("", "".gracefulString())
        assertEquals("xxx", "xxx".gracefulString())
        assertEquals("123", 123.gracefulString())
        assertEquals("123.456", 123.456.gracefulString())
        assertEquals("abc,123.456", arrayOf("abc", 123.456).gracefulString())
        assertEquals("123.456,abc", listOf(123.456, "abc").gracefulString())
        assertEquals("123.456,abc,abc,123.456", listOf(123.456, "abc", arrayOf("abc", 123.456)).gracefulString())
    }
}