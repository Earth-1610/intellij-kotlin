package com.itangcent.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
    fun testMapAsHashMap() {
        val map = mapOf("a" to 1, "b" to 2)
        val hashMap = map.asHashMap()
        assertIs<HashMap<*,*>>(hashMap)
        Assertions.assertEquals(mapOf("a" to 1, "b" to 2), hashMap)
    }

    @Test
    fun testEmptyMap() {
        val emptyMap = emptyMap<String, Int>()
        val hashMap = emptyMap.asHashMap()
        assertIs<HashMap<*,*>>(hashMap)
        assertTrue(hashMap.isEmpty())
    }

    @Test
    fun testSingleMap() {
        val singleMap = mapOf("a" to 1)
        val hashMap = singleMap.asHashMap()
        assertIs<HashMap<*,*>>(hashMap)
        Assertions.assertEquals(mapOf("a" to 1), hashMap)
    }

    @Test
    fun testListAsArrayList() {
        val list = listOf(1, 2, 3)
        val arrayList = list.asArrayList()
        assertIs<ArrayList<*>>(arrayList)
        Assertions.assertEquals(listOf(1, 2, 3), arrayList)
    }

    @Test
    fun testEmptyList() {
        val emptyList = emptyList<Int>()
        val arrayList = emptyList.asArrayList()
        assertIs<ArrayList<*>>(arrayList)
        assertTrue(arrayList.isEmpty())
    }

    @Test
    fun testSingleList() {
        val singleList = listOf(1)
        val arrayList = singleList.asArrayList()
        assertIs<ArrayList<*>>(arrayList)
        Assertions.assertEquals(listOf(1), arrayList)
    }

    @Test
    fun testArrayList() {
        val list = arrayListOf(1, 2, 3)
        val arrayList = list.asArrayList()
        assertIs<ArrayList<*>>(arrayList)
        Assertions.assertEquals(listOf(1, 2, 3), arrayList)
    }
}