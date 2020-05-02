package com.itangcent.test

import com.itangcent.common.utils.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class DateKitTest {

    @Test
    fun testAsDate() {
        val date = Date()
        val time = date.time
        assertEquals(date, time.asDate())
    }

    @Test
    fun testFormat() {
        val time = 1577811661000
        val date = time.asDate()

        assertEquals("2020-01-01", date.formatYMD())
        assertEquals("2020-01-01 01:01:01", date.formatYMD_HMS())
        assertEquals("2020-01-04 01:01:01", date.addDay(3).formatYMD_HMS())
        assertEquals("2020-01-01 04:01:01", date.addHour(3).formatYMD_HMS())
        assertEquals("2020-01-01 01:04:01", date.addMinute(3).formatYMD_HMS())
        assertEquals("2020-04-01 01:01:01", date.addMonth(3).formatYMD_HMS())

    }

}