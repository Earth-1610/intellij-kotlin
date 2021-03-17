package com.itangcent.common.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test case of [MultiValuesMap]
 */
class MultiValuesMapTest {

    private var map = multiValuesMapOf<Any, Any?>()

    @BeforeEach
    fun before() {
        map.put("a", 10)
        map.putAll("b", 20, 21)
        map.putAll("c", 30, 31, 32)
        map.put("d", 40)
        map.remove("d", 40)
    }

    @AfterEach
    fun after() {
        map.clear()
    }


    @Test
    fun testMapBaiscFuns() {
        //containsValue not worked as Map<K, Collection<V>>
        assertFalse(map.containsValue(listOf(10)))
        assertEquals(1, map["a"]!!.size)
        assertEquals(2, map["b"]!!.size)
        assertEquals(3, map["c"]!!.size)
        assertNull(map["d"])
    }

    @Test
    fun testIsEmpty() {
        assertFalse(map.isEmpty())
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun testReplace() {
        map.replace("a", 1)
        map.replace("b", 2)
        map.replace("e", 50)
        assertEquals(map.getOne("a"), 1)
        assertEquals(map.getOne("b"), 2)
        assertEquals(map.getOne("e"), 50)
    }

    @Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
    @Test
    fun testFlattenValues() {
        assertEquals(listOf(10, 20, 21, 30, 31, 32), ArrayList(map.flattenValues()))
    }

    @Test
    fun testFlattenForEach() {
        val sb = StringBuilder()
        map.flattenForEach { k, v ->
            sb.append("$k:$v,")
        }
        assertEquals("a:10,b:20,b:21,c:30,c:31,c:32,", sb.toString())
    }

    @Test
    fun testFlattenForEachWithKeyFilter() {
        val sb = StringBuilder()
        map.flattenForEach({ it is String && it > "a" }) { k, v ->
            sb.append("$k:$v,")
        }
        assertEquals("b:20,b:21,c:30,c:31,c:32,", sb.toString())
    }

    @Test
    fun testRemove() {
        map.remove("b", 20)
        assertEquals(21, map.getFirst("b"))
        map.removeAll("c")
        assertNull(map.getFirst("c"))
    }

    @Test
    fun testGet() {
        assertEquals(10, map.getFirst("a"))
        assertEquals(20, map.getFirst("b"))
        assertEquals(30, map.getFirst("c"))
        assertNull(map.getFirst("d"))

        assertEquals(10, map.getFirst("a"))
        assertThrows<IllegalArgumentException> { map.getOne("b") }
        assertThrows<IllegalArgumentException> { map.getOne("c") }
        assertNull(map.getOne("d"))
    }
}