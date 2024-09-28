package com.itangcent.common.utils

import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

/**
 * @author tangcent
 * @date 2024/09/28
 */
class ConversionServiceTest {

    @Test
    fun `test convert Boolean`() {
        val result = ConversionService.convert("true", Boolean::class.java)
        assertEquals(true, result)
    }

    @Test
    fun `test convert Short`() {
        val result = ConversionService.convert("123", Short::class.java)
        assertEquals(123.toShort(), result)
    }

    @Test
    fun `test convert Integer`() {
        val result = ConversionService.convert("123", Int::class.java)
        assertEquals(123, result)
    }

    @Test
    fun `test convert Long`() {
        val result = ConversionService.convert("123456789", Long::class.java)
        assertEquals(123456789L, result)
    }

    @Test
    fun `test convert Float`() {
        val result = ConversionService.convert("123.45", Float::class.java)
        assertEquals(123.45f, result)
    }

    @Test
    fun `test convert Double`() {
        val result = ConversionService.convert("123.456", Double::class.java)
        assertEquals(123.456, result)
    }

    @Test
    fun `test convert String`() {
        val result = ConversionService.convert("test string", String::class.java)
        assertEquals("test string", result)
    }

    @Test
    fun `test convert date`() {
        val result = ConversionService.convert("2024-09-28 01:02:03", Date::class.java)
        assertEquals("2024-09-28 01:02:03", DateUtils.formatDate(result as Date, DateUtils.YMD_HMS))
    }

    @Test
    fun `test convert enum class`() {
        val result = ConversionService.convert("VALUE1", TestEnum::class.java)
        assertEquals(TestEnum.VALUE1, result)
    }

    @Test
    fun `test convert using GsonConverter`() {
        // Assuming GsonUtils can deserialize a JSON string to a custom class (e.g., TestClass)
        val json = """{"name":"Test"}"""
        val result = ConversionService.convert(json, TestClass::class.java)
        assertTrue(result is TestClass)
        assertEquals("Test", (result as TestClass).name)
    }

    @Test
    fun `test unsupported type throws exception`() {
        assertThrows<JsonSyntaxException> {
            ConversionService.convert("value", IllegalClass::class.java)
        }
    }

    @Test
    fun `test invalid input for number conversions`() {
        assertThrows<NumberFormatException> {
            ConversionService.convert("invalid", Int::class.java)
        }

        assertThrows<NumberFormatException> {
            ConversionService.convert("invalid", Long::class.java)
        }

        assertThrows<NumberFormatException> {
            ConversionService.convert("invalid", Short::class.java)
        }

        assertThrows<NumberFormatException> {
            ConversionService.convert("invalid", Float::class.java)
        }

        assertThrows<NumberFormatException> {
            ConversionService.convert("invalid", Double::class.java)
        }
    }

    // New tests for tryConvert method

    @Test
    fun `test tryConvert with valid conversion`() {
        val result = ConversionService.tryConvert("123", Int::class.java)
        assertEquals(123, result)
    }

    @Test
    fun `test tryConvert with unsupported type returns null`() {
        val result = ConversionService.tryConvert("value", IllegalClass::class.java)
        assertNull(result)
    }

    @Test
    fun `test tryConvert with invalid input returns null`() {
        val result = ConversionService.tryConvert("invalid", Int::class.java)
        assertNull(result)
    }

    @Test
    fun `test tryConvert with empty input returns expected result`() {
        val result = ConversionService.tryConvert("", Boolean::class.java)
        assertEquals(false, result)  // Since "toBoolean()" on empty string returns false
    }
}

class TestClass(val name: String)

class IllegalClass

enum class TestEnum {
    VALUE1,
    VALUE2
}