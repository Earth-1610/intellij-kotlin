package com.itangcent.common.utils

import com.itangcent.common.utils.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals


/**
 * Test case of [com.itangcent.common.utils.MapKit]
 */
class MapKitTest {

    @Test
    fun testSafeComputeIfAbsent() {
        val map = hashMapOf("a" to 1, "b" to 2)
        map.safeComputeIfAbsent("c") { 3 }
        assertEquals(3, map["c"])
        map.safeComputeIfAbsent("a") { 2 }
        assertEquals(1, map["a"])
        assertDoesNotThrow { ConcurrentHashMap<String, Int>().safeComputeIfAbsent("a") { 1 } }
        assertDoesNotThrow { map.safeComputeIfAbsent("e") { throw NullPointerException() } }
        assertDoesNotThrow { map.safeComputeIfAbsent("f") { throw ConcurrentModificationException() } }
    }

    @Test
    fun testAny() {
        val map = hashMapOf("a" to 1, "b" to 2)
        assertEquals(1, map.any("a"))
        assertEquals(1, map.any("a", "b"))
        assertEquals(2, map.any("b", "a"))
        assertEquals(2, map.any("c", "b"))
        assertEquals(null, map.any("c", "d"))
        assertEquals(null, null.any("c", "b"))
    }

    @Test
    fun testAppend() {
        val map: HashMap<String, String?> = hashMapOf("a" to "1", "b" to "2")
        map.append("a", "2")
        assertEquals("1 2", map["a"])
        map.append("b", "2", ",")
        assertEquals("2,2", map["b"])
        map.append("c", "3")
        assertEquals("3", map["c"])
        assertDoesNotThrow { (null as MutableMap<Any, String?>?).append("a", "1") }
    }

    @Test
    fun testFlat() {
        val map: HashMap<Any?, Any?> = linkedMapOf(
            "a" to "1",
            "b" to linkedMapOf(
                "c" to "3",
                "d" to "4"
            ),
            5 to "e",
            null to 6,
            "null" to null,
            "list" to listOf("g", "h", linkedMapOf("i" to 9, "j" to 10), null),
            "map" to linkedMapOf(
                "k" to 11, "l" to 12, "m" to listOf(13, 14)
            ),
            "array" to arrayOf("n", "o", linkedMapOf("p" to 16, "q" to 17), null)
        )

        val sb = StringBuilder()
        map.flat { path, value ->
            sb.append("$path:$value,")
        }
        assertEquals(
            "a:1,b.c:3,b.d:4,5:e," +
                    "list:g,list:h,list.i:9,list.j:10," +
                    "map.k:11,map.l:12,map.m:13,map.m:14," +
                    "array:n,array:o,array.p:16,array.q:17,",
            sb.toString()
        )
        assertEquals(
            linkedMapOf(
                "a" to "1", "b.c" to "3", "b.d" to "4", "5" to "e",
                "list" to "g", "list" to "h", "list.i" to "9", "list.j" to "10",
                "map.k" to "11", "map.l" to "12", "map.m" to "13", "map.m" to "14",
                "array" to "n", "array" to "o", "array.p" to "16", "array.q" to "17"
            ),
            map.flat()
        )
        assertEquals(
            multiValuesMapOf(
                "a" to "1", "b.c" to "3", "b.d" to "4", "5" to "e",
                "list" to "g", "list" to "h", "list.i" to "9", "list.j" to "10",
                "map.k" to "11", "map.l" to "12", "map.m" to "13", "map.m" to "14",
                "array" to "n", "array" to "o", "array.p" to "16", "array.q" to "17"
            ),
            map.flatMulti()
        )
    }
}