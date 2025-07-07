package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

/**
 * Test case of IOKit
 */
class IOKitTest {

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

    @Test
    fun testAsUrl() {
        assertEquals(
            "https://example.com",
            "https://example.com".asUrl().toString()
        )
        assertEquals(
            "http://localhost:8080/path?query=param",
            "http://localhost:8080/path?query=param".asUrl().toString()
        )
    }

}
