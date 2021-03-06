package com.itangcent.test

import com.itangcent.common.utils.KV
import com.itangcent.common.utils.getAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Test case for [KV.getAs]
 */
@RunWith(JUnit4::class)
class KVMapTest {

    private var map = HashMap<Any?, Any?>()

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
    fun testGetAs() {
        map["string"] = "string"
        map["kv"] = KV.by(2, 2)

        assertEquals(null, map.getAs<String>(1))
        assertEquals(null, map.getAs<Int>("string"))
        assertEquals(1, map.getAs<Int>(1))
        assertEquals("string", map.getAs<String>("string"))

    }

    @Test
    fun testSubGetAs() {
        map["string"] = "string"
        map["kv"] = KV<Any?, Any?>().set(3, 3).set("string", "string")

        assertEquals(null, map.getAs<String>(1, 1))
        assertEquals(null, map.getAs<Int>("string", "string"))
        assertEquals(null, map.getAs<String>("kv", 3))
        assertEquals(null, map.getAs<Int>("kv", "string"))
        assertEquals(3, map.getAs<Int>("kv", 3))
        assertEquals("string", map.getAs<String>("kv", "string"))
    }

    @Test
    fun testGrandGetAs() {
        map["string"] = "string"
        map["kv"] = KV<Any?, Any?>().set(3, 3).set("string", "string")
            .set("kv", KV<Any?, Any?>().set(4, 4).set("string", "string"))

        assertEquals(null, map.getAs<String>(1, 1, 1))
        assertEquals(null, map.getAs<Int>("string", "string", "string"))
        assertEquals(null, map.getAs<String>("kv", "kv", 4))
        assertEquals(null, map.getAs<Int>("kv", "kv", "string"))
        assertEquals(4, map.getAs<Int>("kv", "kv", 4))
        assertEquals("string", map.getAs<String>("kv", "kv", "string"))
    }

}