package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow


/**
 * Test case of [IOUtils]
 */
class IOUtilsTest {

    @Test
    fun testCloseQuietly() {
        assertDoesNotThrow {
            IOUtils.closeQuietly(null, { throw IllegalStateException("failed close") })
        }
    }
}