package com.itangcent.common.utils

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
        assertEquals(
            "{\"1\":1,\"2\":\"2\",\"3\":\"<>&='\"}", GsonUtils.toJson(
                linkedMapOf(
                    1 to 1,
                    "2" to "2",
                    "3" to "<>&='"
                )
            )
        )
        assertEquals("{\"int\":10}", GsonUtils.toJson(linkedMapOf("int" to 10)))
    }

    @Test
    fun testFromJson() {
        assertEquals("{\"x\":1,\"y\":2}", GsonUtils.toJson(GsonUtilsTestPoint(1, 2)))
        assertEquals(GsonUtilsTestPoint(1, 2), GsonUtils.fromJson("{\"x\":1,\"y\":2}", GsonUtilsTestPoint::class))
        assertEquals("{\"int\":10}", GsonUtils.toJson(GsonUtils.fromJson("{\"int\":10}")))
    }

    @Test
    fun testToJsonSafely() {
        val map = linkedMapOf<Any, Any>(1 to 1, "2" to "2")
        map["recursion"] = map
        assertThrows(StackOverflowError::class.java) { GsonUtils.toJson(map) }
        assertDoesNotThrow { GsonUtils.toJsonSafely(map) }
        assertEquals(
            "{\"1\":1,\"2\":\"2\",\"recursion\":{\"1\":1,\"2\":\"2\",\"recursion\":{}}}",
            GsonUtils.toJsonSafely(map)
        )
    }

    @Test
    fun testToJsonWithNulls() {
        val map = linkedMapOf(1 to 1, "2" to null)
        assertEquals("{\"1\":1}", GsonUtils.toJson(map))
        assertEquals("{\"1\":1,\"2\":null}", GsonUtils.toJsonWithNulls(map))
    }

    @Test
    fun parseToJsonTree() {
        val jsonElement = GsonUtils.parseToJsonTree("{1:1,\"2\":null}")
        assertNotNull(jsonElement)
        assertTrue(jsonElement!!.isJsonObject)
        val jsonObject = jsonElement.asJsonObject
        assertNotNull(jsonObject)
        assertEquals(1, jsonObject.getAsJsonPrimitive("1").asInt)
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
                    "}", GsonUtils.prettyJson(linkedMapOf(1 to 1, "2" to null))
        )
    }

    @Test
    fun testPrettyJsonSafely() {
        val map = linkedMapOf<Any, Any>(1 to 1, "2" to "2")
        map["recursion"] = map
        assertThrows(StackOverflowError::class.java) { GsonUtils.prettyJson(map) }
        assertDoesNotThrow { GsonUtils.prettyJsonSafely(map) }
        assertEquals(
            "{\n" +
                    "  \"1\": 1,\n" +
                    "  \"2\": \"2\",\n" +
                    "  \"recursion\": {\n" +
                    "    \"1\": 1,\n" +
                    "    \"2\": \"2\",\n" +
                    "    \"recursion\": {}\n" +
                    "  }\n" +
                    "}", GsonUtils.prettyJsonSafely(map)
        )
    }

    @Test
    fun testPrettyJsonWithNulls() {
        val map = linkedMapOf(1 to 1, "2" to null)
        assertEquals(
            "{\n" +
                    "  \"1\": 1\n" +
                    "}", GsonUtils.prettyJson(map)
        )
        assertEquals(
            "{\n" +
                    "  \"1\": 1,\n" +
                    "  \"2\": null\n" +
                    "}", GsonUtils.prettyJsonWithNulls(map)
        )
    }

}

data class GsonUtilsTestPoint(var x: Int, var y: Int)
