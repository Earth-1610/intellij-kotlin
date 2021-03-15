package com.itangcent.test

import com.itangcent.common.utils.IOUtils
import com.itangcent.common.utils.readString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.ByteArrayInputStream
import java.io.Closeable
import kotlin.test.assertEquals


/**
 * Test case of [IOUtils]
 */
class IOUtilsTest {

    @Test
    fun testCloseQuietly() {
        assertDoesNotThrow {
            IOUtils.closeQuietly(object : Closeable {
                override fun close() {
                    throw IllegalStateException("failed close")
                }
            })
        }
    }

    @Test
    fun testReadString() {
        assertEquals(
            "hello world", ByteArrayInputStream("hello world".toByteArray(Charsets.UTF_8))
                .readString()
        )
        assertEquals(
            "hello world", ByteArrayInputStream("hello world".toByteArray(Charsets.UTF_8))
                .readString(Charsets.UTF_8)
        )
    }
}