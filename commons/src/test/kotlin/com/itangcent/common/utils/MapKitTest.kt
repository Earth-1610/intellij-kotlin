package com.itangcent.common.utils

import org.junit.jupiter.api.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals


/**
 * Test case of [com.itangcent.common.utils.MapKit]
 */
class MapKitTest {

    private var map = KV.any()

    @BeforeEach
    fun before() {
        map[1] = 1
        map[2] = 2
    }

    @AfterEach
    fun after() {
        map.clear()
    }

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

    @Test
    fun testMutable() {
        Assertions.assertEquals(1, mapOf(1 to 1, 2 to 2).mutable()[1])
        hashMapOf(1 to 1, 2 to 2).let {
            Assertions.assertSame(it, it.mutable())
            Assertions.assertNotSame(it, it.mutable(true))
            Assertions.assertEquals(it, it.mutable(true))
        }
    }

    @Test
    fun testMerge() {
        //simple
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3),
            hashMapOf(1 to 1, 2 to 2).merge(mapOf(1 to 2, 3 to 3))
        )


        //merge sub map
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to mapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to mapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3)))
        )


        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyMap<Int, Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3))
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyMap<Int, Int>()))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyMap<Int, Int>()),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyMap<Int, Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyMap<Int, Int>()))
        )

        //merge sub list
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to arrayListOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to arrayListOf(2, 3)))
        )
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to arrayListOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to listOf(2, 3)))
        )
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to arrayListOf(2, 3)))
        )
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to listOf(2, 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyList<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to listOf(2, 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(2, 3))
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyList<Int>()))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyList<Int>()),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyList<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyList<Int>()))
        )

        //merge sub set
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashSetOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashSetOf(2, 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashSetOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to setOf(2, 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashSetOf(2, 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to setOf(2, 3)))
        )

        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptySet<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to setOf(2, 3)))
        )
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(2, 3))
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptySet<Int>()))
        )
        Assertions.assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptySet<Int>()),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptySet<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptySet<Int>()))
        )
    }

    @Test
    fun trySet() {
        assertEquals(
            mapOf("x" to 1),
            mapOf("x" to 1).also {
                it.trySet("y", 2)
            }
        )
        assertEquals(
            hashMapOf("x" to 1, "y" to 2),
            hashMapOf("x" to 1).also {
                it.trySet("y", 2)
            }
        )
    }

    @Test
    fun testGetAs() {
        map["string"] = "string"
        map["kv"] = KV.by(2, 2)

        Assertions.assertEquals(null, map.getAs<String>(1))
        Assertions.assertEquals(null, map.getAs<Int>("string"))
        Assertions.assertEquals(1, map.getAs<Int>(1))
        Assertions.assertEquals("string", map.getAs<String>("string"))

    }

    @Test
    fun testSubGetAs() {
        map["string"] = "string"
        map["kv"] = linkedMapOf(3 to 3, "string" to "string")

        Assertions.assertEquals(null, map.getAs<String>(1, 1))
        Assertions.assertEquals(null, map.getAs<Int>("string", "string"))
        Assertions.assertEquals(null, map.getAs<String>("kv", 3))
        Assertions.assertEquals(null, map.getAs<Int>("kv", "string"))
        Assertions.assertEquals(3, map.getAs<Int>("kv", 3))
        Assertions.assertEquals("string", map.getAs<String>("kv", "string"))
    }

    @Test
    fun testGrandGetAs() {
        map["string"] = "string"
        map["kv"] = linkedMapOf(3 to 3, "string" to "string", "kv" to linkedMapOf(4 to 4, "string" to "string"))


        Assertions.assertEquals(null, map.getAs<String>(1, 1, 1))
        Assertions.assertEquals(null, map.getAs<Int>("string", "string", "string"))
        Assertions.assertEquals(null, map.getAs<String>("kv", "kv", 4))
        Assertions.assertEquals(null, map.getAs<Int>("kv", "kv", "string"))
        Assertions.assertEquals(4, map.getAs<Int>("kv", "kv", 4))
        Assertions.assertEquals("string", map.getAs<String>("kv", "kv", "string"))
    }

    @Test
    fun testGetSub() {
        map["string"] = "string"
        map["sub"] = linkedMapOf(3 to 3, "string" to "string", "sub" to linkedMapOf(4 to 4, "string" to "string2"))

        Assertions.assertEquals(null, map.getSub("string"))
        Assertions.assertEquals("string", map.getSub("sub")!!["string"])
        Assertions.assertEquals("string2", map.getSub("sub")!!.getSub("sub")!!["string"])
    }
}