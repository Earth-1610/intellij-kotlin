package com.itangcent.test

import com.itangcent.common.utils.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


/**
 * Test case of [StringKit]
 */
class StringKitTest {

    @Test
    fun testToBool() {
        Assertions.assertEquals(true, "1".toBool())
        Assertions.assertEquals(true, "true".toBool())
        Assertions.assertEquals(false, "0".toBool())
        Assertions.assertEquals(false, "999".toBool())
        Assertions.assertEquals(false, "false".toBool())
        Assertions.assertEquals(false, "abcd".toBool())
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
        Assertions.assertEquals("1", "1".flatten())
        Assertions.assertEquals("1\\n2", "1${n}2".flatten())
        Assertions.assertEquals("1\\n2\\n3", "1${n}2${n}3".flatten())
    }

    @Test
    fun testAppendln() {
        Assertions.assertEquals(null, null.appendln())
        Assertions.assertEquals("", "".appendln())
        Assertions.assertEquals("1$n", "1".appendln())
        Assertions.assertEquals("1$n", "1${n}".appendln())
        Assertions.assertEquals("new-line", null.appendln("new-line"))
        Assertions.assertEquals("new-line", "".appendln("new-line"))
        Assertions.assertEquals("1${n}new-line", "1".appendln("new-line"))
        Assertions.assertEquals("1$n${n}new-line", "1${n}".appendln("new-line"))
    }

    private val n = System.getProperty("line.separator")
}