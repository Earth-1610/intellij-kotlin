package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Test case of [CollectionUtils]
 */
class CollectionUtilsTest {

    @Test
    fun testContainsAny() {
        assertFalse(CollectionUtils.containsAny(emptyArray<String>(), emptyList<String>()))
        assertFalse(CollectionUtils.containsAny(emptyArray<String>(), listOf("")))
        assertFalse(CollectionUtils.containsAny(emptyArray<String>(), listOf(null)))

        assertFalse(CollectionUtils.containsAny(arrayOf("a", "b"), emptyList<String>()))
        assertFalse(CollectionUtils.containsAny(arrayOf("a", "b"), listOf("")))
        assertFalse(CollectionUtils.containsAny(arrayOf("a", "b"), listOf(null)))
        assertTrue(CollectionUtils.containsAny(arrayOf("a", "b", null), listOf(null)))
        assertTrue(CollectionUtils.containsAny(arrayOf("a", "b"), listOf("a")))
        assertTrue(CollectionUtils.containsAny(arrayOf("a", "b"), listOf("c", "b")))
        assertTrue(CollectionUtils.containsAny(arrayOf("a", "b"), listOf("d", "c", "b")))

        assertFalse(CollectionUtils.containsAny(listOf("a", "b"), ""))
        assertFalse(CollectionUtils.containsAny(listOf("a", "b"), null))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b", null), null))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b"), "a"))
        assertFalse(CollectionUtils.containsAny(listOf("a", "b"), "c"))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b"), "c", "b"))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b"), "d", "c", "b"))

        assertFalse(CollectionUtils.containsAny(listOf("a", "b"), emptyList<String>()))
        assertFalse(CollectionUtils.containsAny(listOf("a", "b"), listOf("")))
        assertFalse(CollectionUtils.containsAny(listOf("a", "b"), listOf(null)))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b", null), listOf(null)))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b"), listOf("a")))
        assertTrue(CollectionUtils.containsAny(listOf("a", "b", "a"), listOf("c", "b")))
        assertTrue(CollectionUtils.containsAny(listOf("c", "b"), listOf("a", "b", "a")))
    }

    @Test
    fun testContainsAll() {
        assertTrue(CollectionUtils.containsAll(emptyArray<String>(), emptyList<String>()))
        assertFalse(CollectionUtils.containsAll(emptyArray<String>(), listOf("")))
        assertFalse(CollectionUtils.containsAll(emptyArray<String>(), listOf(null)))

        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b"), emptyList<String>()))
        assertFalse(CollectionUtils.containsAll(arrayOf("a", "b"), listOf("")))
        assertFalse(CollectionUtils.containsAll(arrayOf("a", "b"), listOf(null)))
        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b", null), listOf(null)))
        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b"), listOf("a")))
        assertFalse(CollectionUtils.containsAll(arrayOf("a", "b"), listOf("c", "b")))

        assertTrue(CollectionUtils.containsAll(arrayOf<String?>(null), emptyList<String>()))
        assertTrue(CollectionUtils.containsAll(arrayOf<String?>(null), listOf(null)))
        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b"), listOf("a")))
        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b"), listOf("a", "b")))
        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b", "a"), listOf("a", "b")))
        assertTrue(CollectionUtils.containsAll(arrayOf("a", "b", null), listOf(null)))
        assertFalse(CollectionUtils.containsAll(arrayOf("a", "b"), listOf("c", "b")))

    }

    @Test
    fun testIsEmpty() {
        assertTrue(CollectionUtils.isEmpty(null))
        assertTrue(CollectionUtils.isEmpty(emptyList<String>()))
        assertFalse(CollectionUtils.isEmpty(listOf("a", "b", "a")))
        assertFalse(CollectionUtils.isEmpty(listOf(null)))

    }

}