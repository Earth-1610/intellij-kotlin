package com.itangcent.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Test case of [CollectionKit]
 */
class CollectionKitTest {

    @Test
    fun testReduceSafely() {
        assertEquals(null, emptyList<String>().reduceSafely { s1, s2 -> "$s1\n$s2" })
        assertEquals("a", listOf("a").reduceSafely { s1, s2 -> "$s1,$s2" })
        assertEquals(null, listOf<String?>(null).reduceSafely { s1, s2 -> "$s1,$s2" })
        assertEquals("a,null", listOf("a", null).reduceSafely { s1, s2 -> "$s1,$s2" })
        assertEquals("null,a", listOf(null, "a").reduceSafely { s1, s2 -> "$s1,$s2" })
        assertEquals("a,b", listOf("a", "b").reduceSafely { s1, s2 -> "$s1,$s2" })
        assertEquals("a,b,c", listOf("a", "b", "c").reduceSafely { s1, s2 -> "$s1,$s2" })
    }

    @Test
    fun testNotNullOrEmpty() {
        //array
        assertFalse((null as Array<*>?).notNullOrEmpty())
        assertFalse(emptyArray<String>().notNullOrEmpty())
        assertTrue(arrayOf("a", "b", "a").notNullOrEmpty())
        assertTrue(arrayOf<String?>(null).notNullOrEmpty())

        //collection
        assertFalse((null as Collection<*>?).notNullOrEmpty())
        assertFalse(emptyList<String>().notNullOrEmpty())
        assertTrue(listOf("a", "b", "a").notNullOrEmpty())
        assertTrue(listOf(null).notNullOrEmpty())

        //map
        assertFalse((null as Map<*, *>?).notNullOrEmpty())
        assertFalse(emptyMap<String, String>().notNullOrEmpty())
        assertTrue(mapOf("a" to 1).notNullOrEmpty())
        assertTrue(mapOf(null to null).notNullOrEmpty())
    }

    @Test
    fun testAsHashMap() {
        val map = mapOf("x" to 1)
        val hashMap = map.asHashMap()
        Assertions.assertEquals(HashMap::class, hashMap::class)
        Assertions.assertFalse(map === hashMap)
        Assertions.assertEquals(map, hashMap)
        Assertions.assertTrue(hashMap === hashMap.asHashMap())
    }

    @Test
    fun testAsArrayList() {
        val list = listOf("x", "y")
        val arrayList = list.asArrayList()
        Assertions.assertEquals(ArrayList::class, arrayList::class)
        Assertions.assertFalse(list === arrayList)
        Assertions.assertEquals(list, arrayList)
        Assertions.assertTrue(arrayList === arrayList.asArrayList())
    }
}