package com.itangcent.common.utils

import com.itangcent.common.utils.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateKitTest {

    @Test
    fun testAsDate() {
        val date = Date()
        val time = date.time
        assertEquals(date, time.asDate())
    }

    @Test
    fun testFormat() {
        val time = DateUtils.parseYMD_HMS("2020-01-01 01:01:01").time
        val date = time.asDate()

        assertEquals("2020-01-01", date.formatYMD())
        assertEquals("2020-01-01 01:01:01", date.formatYMD_HMS())
        assertEquals("2020-01-04 01:01:01", date.addDay(3).formatDate("yyyy-MM-dd HH:mm:ss"))
        assertEquals("2020-01-01 04:01:01", date.addHour(3).formatYMD_HMS())
        assertEquals("2020-01-01 01:04:01", date.addMinute(3).formatYMD_HMS())
        assertEquals("2020-04-01 01:01:01", date.addMonth(3).formatYMD_HMS())

    }

    @Test
    fun testIsSameDay() {
        val date = DateUtils.parseYMD_HMS("2020-01-01 01:01:01")
        assertFalse(date.isSameDay(null))
        assertTrue(date.isSameDay(date))
        assertTrue(date.isSameDay(date.addHour(22)))
        assertFalse(date.isSameDay(date.addHour(23)))
        assertFalse(date.isSameDay(date.addDay(1)))
    }

    @Test
    fun testDayStart() {
        val date = DateUtils.parseYMD_HMS("2020-01-01 01:01:01")
        assertEquals("2020-01-01 00:00:00", date.dayStart().formatYMD_HMS())
        assertEquals("2020-01-01 23:59:59", date.dayEnd().formatYMD_HMS())
    }

}