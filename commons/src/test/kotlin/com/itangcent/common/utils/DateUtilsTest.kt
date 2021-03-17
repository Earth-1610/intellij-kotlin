package com.itangcent.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test case of [DateUtils]
 */
class DateUtilsTest {

    @Test
    fun testCurrentMethods() {
        if (DateUtils.now().formatDate("mm") == "59") {//skip xx:59:xx
            TimeUnit.SECONDS.sleep(1)
        }
        val now = DateUtils.now()
        val time = DateUtils.currentTimeMillis()
        val nowStr = now.formatYMD_HMS()

        //currentMonth
        assertTrue(nowStr.startsWith(DateUtils.currentMonth.formatDate("yyyy-MM")))
        assertTrue(DateUtils.currentMonth.formatYMD_HMS().endsWith("01 00:00:00"))

        //currentDay
        assertTrue(nowStr.startsWith(DateUtils.currentDay.formatYMD()))
        assertTrue(DateUtils.currentDay.formatYMD_HMS().endsWith("00:00:00"))

        //currentHour
        assertTrue(nowStr.startsWith(DateUtils.currentHour.formatDate("yyyy-MM-dd HH")))
        assertTrue(DateUtils.currentHour.formatYMD_HMS().endsWith("00:00"))

        //currentMinute
        assertTrue(nowStr.startsWith(DateUtils.currentMinute.formatDate("yyyy-MM-dd HH:mm")))
        assertTrue(DateUtils.currentMinute.formatYMD_HMS().endsWith("00"))

        assertEquals(DateUtils.currentDay, DateUtils.getCurrentDay(0))
        assertEquals(DateUtils.currentDay, DateUtils.getCurrentDay(time, 0))
        assertEquals(DateUtils.currentDay.addDay(1), DateUtils.getCurrentDay(1))

        assertEquals(DateUtils.currentHour, DateUtils.getCurrentHour(0))
        assertEquals(DateUtils.currentHour.addHour(1), DateUtils.getCurrentHour(1))
    }

    @Test
    fun testFormat() {
        val date = DateUtils.parseYMD_HMS("2020-01-01 01:01:01")
        val time = date.time

        Assertions.assertEquals("2020-01-01", DateUtils.formatYMD(date))
        Assertions.assertEquals("2020-01-01 01:01:01", DateUtils.formatYMD_HMS(date))
        Assertions.assertEquals("2020-01-01 01:01:01", DateUtils.formatDate(date, "yyyy-MM-dd HH:mm:ss"))
        Assertions.assertEquals("2020-01-01 01:01:01", DateUtils.formatDate(time, "yyyy-MM-dd HH:mm:ss"))

    }

    @Test
    fun testParse() {
        val date = DateUtils.parseYMD_HMS("2020-01-01 01:01:01")
        val time = date.time
        val ymd = DateUtils.parseYMD("2020-01-01")
        assertEquals(time - 3661000, ymd!!.time)
        val ymdHm = DateUtils.parse("2020-01-01 01:01", "yyyy-MM-dd HH:mm")
        assertEquals(time - 1000L, ymdHm!!.time)

        //parse without pattern
        assertEquals(time, DateUtils.parse("2020-01-01 01:01:01").time)
        assertEquals(time, DateUtils.parse("2020/01/01 01:01:01").time)
        assertEquals(time - 3661000L, DateUtils.parse("20200101").time)
        assertEquals(time - 3661000L, DateUtils.parse("2020-01-01").time)
        assertEquals(time - 3661000L, DateUtils.parse("2020/01/01").time)
        assertThrows<IllegalArgumentException> { DateUtils.parse("abcd") }
        assertNull(DateUtils.parse("abcd", "yyyy-MM-dd HH:mm"))
    }

    @Test
    fun testConvert() {
        assertEquals(DateUtils.convert(1577811661000L), Date(1577811661000L))
    }
}