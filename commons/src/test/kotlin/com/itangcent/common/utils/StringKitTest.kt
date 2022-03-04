package com.itangcent.common.utils

import com.itangcent.common.utils.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Test case of [StringKit]
 */
class StringKitTest {

    @Test
    fun testCamelToUnderline() {
        Assertions.assertEquals("", StringUtils.camelToUnderline(null))
        Assertions.assertEquals("", StringUtils.camelToUnderline(""))
        Assertions.assertEquals("", StringUtils.camelToUnderline(" "))
        Assertions.assertEquals("a", StringUtils.camelToUnderline("a"))
        Assertions.assertEquals("a", StringUtils.camelToUnderline("A"))
        Assertions.assertEquals("a_b", StringUtils.camelToUnderline("AB"))
        Assertions.assertEquals("a_b", StringUtils.camelToUnderline("aB"))

        Assertions.assertEquals("abcd", StringUtils.camelToUnderline("abcd"))
        Assertions.assertEquals("abcd", StringUtils.camelToUnderline("Abcd"))
        Assertions.assertEquals("a_bcd", StringUtils.camelToUnderline("ABcd"))
        Assertions.assertEquals("a_b_cd", StringUtils.camelToUnderline("ABCd"))
        Assertions.assertEquals("a_b_c_d", StringUtils.camelToUnderline("ABCD"))
        Assertions.assertEquals("a_bcd", StringUtils.camelToUnderline("aBcd"))
        Assertions.assertEquals("a_b_cd", StringUtils.camelToUnderline("aBCd"))
        Assertions.assertEquals("abc_d", StringUtils.camelToUnderline("AbcD"))
    }

    @Test
    fun testUnderlineToCamel() {
        Assertions.assertEquals("", StringUtils.underlineToCamel(null))
        Assertions.assertEquals("", StringUtils.underlineToCamel(""))
        Assertions.assertEquals("", StringUtils.underlineToCamel(" "))
        Assertions.assertEquals("A", StringUtils.underlineToCamel("_a"))
        Assertions.assertEquals("A", StringUtils.underlineToCamel("_a_"))
        Assertions.assertEquals("AB", StringUtils.underlineToCamel("_a_b"))
        Assertions.assertEquals("a", StringUtils.underlineToCamel("a"))
        Assertions.assertEquals("a", StringUtils.underlineToCamel("a_"))
        Assertions.assertEquals("aB", StringUtils.underlineToCamel("a_b"))

        Assertions.assertEquals("Ab", StringUtils.underlineToCamel("_ab"))
        Assertions.assertEquals("Ab", StringUtils.underlineToCamel("_ab_"))
        Assertions.assertEquals("AbCd", StringUtils.underlineToCamel("_ab_cd"))
        Assertions.assertEquals("ab", StringUtils.underlineToCamel("ab"))
        Assertions.assertEquals("ab", StringUtils.underlineToCamel("ab_"))
        Assertions.assertEquals("abCd", StringUtils.underlineToCamel("ab_cd"))
    }

    @Test
    fun testFirstCharacterIndex() {
        Assertions.assertEquals(-1, StringUtils.firstCharacterIndex(""))
        Assertions.assertEquals(-1, StringUtils.firstCharacterIndex(" "))
        Assertions.assertEquals(0, StringUtils.firstCharacterIndex("1"))
        Assertions.assertEquals(1, StringUtils.firstCharacterIndex(" 1"))
    }

    @Test
    fun testToBool() {
        Assertions.assertEquals(false, "".toBool())
        Assertions.assertEquals(true, "".toBool(true))
        Assertions.assertEquals(true, "1".toBool())
        Assertions.assertEquals(true, "true".toBool())
        Assertions.assertEquals(false, "0".toBool())
        Assertions.assertEquals(false, "999".toBool())
        Assertions.assertEquals(false, "false".toBool())
        Assertions.assertEquals(false, "abcd".toBool())
    }

    @Test
    fun testAppendlnIfNotEmpty() {
        Assertions.assertEquals("", StringBuilder().appendlnIfNotEmpty().toString())
        Assertions.assertEquals("", StringBuilder("").appendlnIfNotEmpty().toString())
        Assertions.assertEquals("a\n", StringBuilder("a").appendlnIfNotEmpty().toString())
    }

    @Test
    fun testTruncate() {
        Assertions.assertEquals("1", "1".truncate(3))
        Assertions.assertEquals("12", "12".truncate(3))
        Assertions.assertEquals("123", "123".truncate(3))
        Assertions.assertEquals("123...", "1234".truncate(3))
        Assertions.assertEquals("123...", "12345".truncate(3))
        Assertions.assertEquals("123...", "123456".truncate(3))
        Assertions.assertEquals("1", "1".truncate(3, "*"))
        Assertions.assertEquals("12", "12".truncate(3, "*"))
        Assertions.assertEquals("123", "123".truncate(3, "*"))
        Assertions.assertEquals("123*", "1234".truncate(3, "*"))
        Assertions.assertEquals("123*", "12345".truncate(3, "*"))
        Assertions.assertEquals("123*", "123456".truncate(3, "*"))
    }

    @Test
    fun testTrimToNull() {
        Assertions.assertEquals("1", "1".trimToNull())
        Assertions.assertEquals(null, "".trimToNull())
        Assertions.assertEquals(null, null.trimToNull())
    }

    @Test
    fun testTinyString() {
        Assertions.assertEquals("1", "1".tinyString())
        Assertions.assertEquals("", "".tinyString())
        Assertions.assertEquals(null, null.tinyString())

        Assertions.assertEquals("1", arrayOf<Any?>("1").tinyString())
        Assertions.assertEquals("", arrayOf<Any?>("").tinyString())
        Assertions.assertEquals(null, arrayOf<Any?>(null).tinyString())

        Assertions.assertEquals("1", arrayListOf<Any?>("1").tinyString())
        Assertions.assertEquals("", arrayListOf<Any?>("").tinyString())
        Assertions.assertEquals(null, arrayListOf<Any?>(null).tinyString())
    }

    @Test
    fun testFlatten() {
        Assertions.assertEquals("null", null.flatten())
        Assertions.assertEquals("default", null.flatten("default"))
        Assertions.assertEquals("1", "1".flatten())
        Assertions.assertEquals("1\\n2", "1\n2".flatten())
        Assertions.assertEquals("1\\n2\\n3", "1\n2\n3".flatten())
    }

    @Test
    fun testAppendln() {
        Assertions.assertEquals(null, null.appendln())
        Assertions.assertEquals("", "".appendln())
        Assertions.assertEquals("1\n", "1".appendln())
        Assertions.assertEquals("1\n", "1\n".appendln())
        Assertions.assertEquals("new-line", null.appendln("new-line"))
        Assertions.assertEquals("new-line", "".appendln("new-line"))
        Assertions.assertEquals("1\nnew-line", "1".appendln("new-line"))
        Assertions.assertEquals("1\n\nnew-line", "1\n".appendln("new-line"))
    }

    @Test
    fun testAppend() {
        Assertions.assertEquals(null, null.append(null))
        Assertions.assertEquals("", null.append(""))
        Assertions.assertEquals("abc", null.append("abc"))

        Assertions.assertEquals(null, "".append(null))
        Assertions.assertEquals("", "".append(""))
        Assertions.assertEquals("abc", "".append("abc"))

        Assertions.assertEquals("abc", "abc".append(null))
        Assertions.assertEquals("abc", "abc".append(""))
        Assertions.assertEquals("abc abc", "abc".append("abc"))

        Assertions.assertEquals(null, null.append(null, ","))
        Assertions.assertEquals("", null.append("", ","))
        Assertions.assertEquals("abc", null.append("abc", ","))

        Assertions.assertEquals(null, "".append(null, ","))
        Assertions.assertEquals("", "".append("", ","))
        Assertions.assertEquals("abc", "".append("abc", ","))

        Assertions.assertEquals("abc", "abc".append(null, ","))
        Assertions.assertEquals("abc", "abc".append("", ","))
        Assertions.assertEquals("abc,abc", "abc".append("abc", ","))
    }

    @Test
    fun testNotNullOrEmpty() {
        assertFalse((null as String?).notNullOrEmpty())
        assertFalse("".notNullOrEmpty())
        assertTrue(" ".notNullOrEmpty())
        assertTrue("abc".notNullOrEmpty())
    }

    @Test
    fun testNotNullOrBlank() {
        assertFalse((null as String?).notNullOrBlank())
        assertFalse("".notNullOrBlank())
        assertFalse(" ".notNullOrBlank())
        assertTrue("abc".notNullOrBlank())
    }

    private val n = System.getProperty("line.separator")
}