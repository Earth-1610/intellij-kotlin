package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Test case of [ArrayUtils]
 */
class ArrayUtilsTest {

    @Test
    fun testContains() {
        assertFalse(ArrayUtils.contains(emptyArray<String>(), ""))
        assertFalse(ArrayUtils.contains(emptyArray<String>(), null))

        assertFalse(ArrayUtils.contains(arrayOf("a", "b"), ""))
        assertFalse(ArrayUtils.contains(arrayOf("a", "b"), null))
        assertTrue(ArrayUtils.contains(arrayOf("a", "b"), "a"))
        assertFalse(ArrayUtils.contains(arrayOf("a", "b"), "c"))
    }

    @Test
    fun testIndexOf() {
        assertEquals(-1, ArrayUtils.indexOf(emptyArray<String>(), ""))
        assertEquals(-1, ArrayUtils.indexOf(emptyArray<String>(), null))

        assertEquals(-1, ArrayUtils.indexOf(arrayOf("a", "b"), ""))
        assertEquals(-1, ArrayUtils.indexOf(arrayOf("a", "b"), null))
        assertEquals(2, ArrayUtils.indexOf(arrayOf("a", "b", null), null))
        assertEquals(0, ArrayUtils.indexOf(arrayOf("a", "b"), "a"))
        assertEquals(-1, ArrayUtils.indexOf(arrayOf("a", "b"), "c"))

        assertEquals(-1, ArrayUtils.indexOf(arrayOf("a", "b", "c"), "a", 1))
        assertEquals(1, ArrayUtils.indexOf(arrayOf("a", "b", "c"), "b", 1))
        assertEquals(1, ArrayUtils.indexOf(arrayOf("a", "b", "c"), "b", -1))
        assertEquals(-1, ArrayUtils.indexOf(arrayOf("a", "b", "c"), "b", 10))
        assertEquals(-1, ArrayUtils.indexOf(null, "b", 1))
        assertEquals(-1, ArrayUtils.indexOf(null, "b", -10))
    }

}