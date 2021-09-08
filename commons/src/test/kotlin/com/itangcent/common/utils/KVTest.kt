package com.itangcent.common.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * Test case of [KV]
 */
class KVTest {

    private var kv = KV.any()

    @BeforeEach
    fun before() {
        kv.set(1, 1)[2] = 2
    }

    @AfterEach
    fun after() {
        kv.clear()
    }

    @Test
    fun testCast() {
        assertDoesNotThrow { kv as LinkedHashMap<*, *> }
        assertThrows(ClassCastException::class.java) { HashMap<Any?, Any?>() as KV }
        assertThrows(ClassCastException::class.java) { LinkedHashMap<Any?, Any?>() as KV }
    }

    @Test
    fun testGet() {
        assertEquals(2, kv.size)
        assertEquals(1, kv[1])
        assertEquals(2, kv[2])
    }

    @Test
    fun testSet() {
        kv.set(mapOf(3 to 3))
        kv.set(KV.by(4, 4))
        assertEquals(4, kv.size)
        assertEquals(3, kv[3])
        assertEquals(4, kv[4])
    }

    @Test
    fun testRemove() {
        assertEquals(2, kv.remove(2))
        assertEquals(1, kv.size)
    }

    @Test
    fun testDelete() {
        assertEquals(KV.by(2, 2), kv.delete(1))
        assertEquals(1, kv.size)
    }

    @Test
    fun testSorted() {
        val keys = kv.keys
        assertEquals(1, keys.first())
        assertEquals(2, keys.last())
        val entries = kv.entries
        assertEquals(1 to 1, entries.first().toPair())
        assertEquals(2 to 2, entries.last().toPair())
    }

    @Test
    fun testGetAs() {
        kv["string"] = "string"
        kv["kv"] = KV.by(2, 2)
        assertThrows(ClassCastException::class.java) { kv.getAs<KV<*, *>>("string")[1] }
        assertDoesNotThrow { kv.getAs<KV<*, *>>("kv")[1] }
        assertThrows(ClassCastException::class.java) { kv.getAs<String>("kv").length }
        assertDoesNotThrow { kv.getAs<String>("string").length }
    }

    @Test
    fun testSub() {
        kv["string"] = "string"
        val sub = kv.sub("string")
        Assertions.assertTrue(sub.isEmpty())
        sub["a"] = "a"
        assertEquals(kv.sub("string"), sub)
    }

    @Test
    fun testGetAsKv() {
        kv["string"] = "string"
        kv["kv"] = KV.by(2, 2)
        assertEquals(null, kv.getAsKv("null"))
        assertEquals(KV<Any?, Any?>(), kv.getAsKv("string"))
        assertEquals(KV.by(2, 2), kv.getAsKv("kv"))
    }

    @Test
    fun testAsKV() {
        assertDoesNotThrow { 1.asKV() }
        assertDoesNotThrow { 1L.asKV() }
        assertDoesNotThrow { "str".asKV() }
        assertDoesNotThrow { HashMap<Any?, Any?>().asKV() }
    }

    @Test
    fun testAsKVWithFailed() {
        var kvCreateCnt = 0
        1.asKV { ++kvCreateCnt }
        assertEquals(1, kvCreateCnt)
        1L.asKV { ++kvCreateCnt }
        assertEquals(2, kvCreateCnt)
        "str".asKV { ++kvCreateCnt }
        assertEquals(3, kvCreateCnt)
        null.asKV { ++kvCreateCnt }
        assertEquals(4, kvCreateCnt)
        HashMap<Any?, Any?>().asKV { ++kvCreateCnt }
        assertEquals(4, kvCreateCnt)
    }

    @Test
    fun testMutable() {
        assertEquals(1, mapOf(1 to 1, 2 to 2).mutable()[1])
        hashMapOf(1 to 1, 2 to 2).let {
            assertSame(it, it.mutable())
            assertNotSame(it, it.mutable(true))
            assertEquals(it, it.mutable(true))
        }
    }

    @Test
    fun testClone() {
        kv.clone().let {
            assertEquals(kv, it)
            assertEquals(kv.hashCode(), it.hashCode())
        }
    }

    @Test
    fun testMerge() {
        //simple
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3),
            hashMapOf(1 to 1, 2 to 2).merge(mapOf(1 to 2, 3 to 3))
        )


        //merge sub map
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to mapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 1, 2 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to mapOf(1 to 1, 2 to 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3)))
        )


        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyMap<Int, Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashMapOf(1 to 2, 3 to 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to mapOf(1 to 2, 3 to 3))
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyMap<Int, Int>()))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyMap<Int, Int>()),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyMap<Int, Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyMap<Int, Int>()))
        )

        //merge sub list
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to arrayListOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to arrayListOf(2, 3)))
        )
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to arrayListOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to listOf(2, 3)))
        )
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to arrayListOf(2, 3)))
        )
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to listOf(2, 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyList<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to listOf(2, 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to listOf(2, 3))
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyList<Int>()))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyList<Int>()),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptyList<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptyList<Int>()))
        )

        //merge sub set
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashSetOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashSetOf(2, 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to hashSetOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to setOf(2, 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to hashSetOf(2, 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(1, 2))
                .merge(mapOf(1 to 2, 3 to 3, 4 to setOf(2, 3)))
        )

        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptySet<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to setOf(2, 3)))
        )
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(2, 3)),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to setOf(2, 3))
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptySet<Int>()))
        )
        assertEquals(
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptySet<Int>()),
            hashMapOf(1 to 1, 2 to 2, 3 to 3, 4 to emptySet<Int>())
                .merge(mapOf(1 to 2, 3 to 3, 4 to emptySet<Int>()))
        )
    }
}