package com.itangcent.test

import com.itangcent.common.utils.GsonUtils
import com.itangcent.common.utils.KV
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


/**
 * Test case of [GsonUtils]
 */
class GsonUtilsTest {

    @Test
    fun testToJson() {
        assertEquals("123", GsonUtils.toJson(123))
        assertEquals("\"123\"", GsonUtils.toJson("123"))
        assertEquals("{\"1\":1,\"2\":\"2\"}", GsonUtils.toJson(KV.any().set(1, 1).set("2", "2")))
    }

    @Test
    fun testFromJson() {
        assertEquals("{\"x\":1,\"y\":2}", GsonUtils.toJson(GsonUtilsTestPoint(1, 2)))
        assertEquals(GsonUtilsTestPoint(1, 2), GsonUtils.fromJson("{\"x\":1,\"y\":2}", GsonUtilsTestPoint::class))
    }

    @Test
    fun testToJsonSafely() {
        val kv = KV.any().set(1, 1).set("2", "2")
        kv["recursion"] = kv
        assertThrows(StackOverflowError::class.java) { GsonUtils.toJson(kv) }
        assertDoesNotThrow { GsonUtils.toJsonSafely(kv) }
        assertEquals("{\"1\":1,\"2\":\"2\",\"recursion\":{}}", GsonUtils.toJsonSafely(kv))
    }

    @Test
    fun testToJsonWithNulls() {
        val kv = KV.any().set(1, 1).set("2", null)
        assertEquals("{\"1\":1}", GsonUtils.toJson(kv))
        assertEquals("{\"1\":1,\"2\":null}", GsonUtils.toJsonWithNulls(kv))
    }

    @Test
    fun testPrettyJson() {
        assertEquals(
            "{\n" +
                    "  \"x\": 1,\n" +
                    "  \"y\": 2\n" +
                    "}", GsonUtils.prettyJson(GsonUtilsTestPoint(1, 2))
        )
        assertEquals(
            "{\n" +
                    "  \"1\": 1\n" +
                    "}", GsonUtils.prettyJson(KV.any().set(1, 1).set("2", null))
        )
    }

    @Test
    fun testPrettyJsonSafely() {
        val kv = KV.any().set(1, 1).set("2", "2")
        kv["recursion"] = kv
        assertThrows(StackOverflowError::class.java) { GsonUtils.prettyJson(kv) }
        assertDoesNotThrow { GsonUtils.prettyJsonSafely(kv) }
        assertEquals(
            "{\n" +
                    "  \"1\": 1,\n" +
                    "  \"2\": \"2\",\n" +
                    "  \"recursion\": {}\n" +
                    "}", GsonUtils.prettyJsonSafely(kv)
        )
    }

    @Test
    fun testPrettyJsonWithNulls() {
        val kv = KV.any().set(1, 1).set("2", null)
        assertEquals(
            "{\n" +
                    "  \"1\": 1\n" +
                    "}", GsonUtils.prettyJson(kv)
        )
        assertEquals(
            "{\n" +
                    "  \"1\": 1,\n" +
                    "  \"2\": null\n" +
                    "}", GsonUtils.prettyJsonWithNulls(kv)
        )
    }

}

data class GsonUtilsTestPoint(var x: Int, var y: Int)
