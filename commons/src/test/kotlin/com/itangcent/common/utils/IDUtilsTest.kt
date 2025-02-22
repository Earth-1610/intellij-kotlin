package com.itangcent.common.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


/**
 * Test case of [IDUtils]
 */
class IDUtilsTest {

    private val ids = HashSet<String>()

    @AfterEach
    fun after() {
        ids.clear()
    }

    private fun testIDGenerate(length: Int, generator: () -> String) {
        for (i in 0..20) {
            val id = generator()
            assertEquals(length, id.length)
            assertTrue(ids.add(id))
        }
    }

    @Test
    fun testShortUUID() {
        testIDGenerate(16) { IDUtils.shortUUID() }
    }

    @Test
    fun testTimeID() {
        testIDGenerate(32) { IDUtils.timeId() }
    }

    @Test
    fun testLiteId() {
        for (length in 5..64) {
            testIDGenerate(length) { IDUtils.liteId(length, true) }
            testIDGenerate(length) { IDUtils.liteId(length, false) }
        }
    }
}