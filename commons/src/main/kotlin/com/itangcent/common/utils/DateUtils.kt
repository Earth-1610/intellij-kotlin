package com.itangcent.common.utils

import org.apache.commons.lang3.time.FastDateFormat
import java.text.ParseException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object DateUtils {

    const val YMD_HMS = "yyyy-MM-dd HH:mm:ss"

    const val YMD = "yyyy-MM-dd"

    private val regPatternMap = ConcurrentHashMap<Pattern, String>()

    val currentDay: Date
        get() {
            val cal = Calendar.getInstance()

            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            return cal.time
        }

    val currentHour: Date
        get() {
            val cal = Calendar.getInstance()

            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            return cal.time
        }

    val currentMinute: Date
        get() {
            val cal = Calendar.getInstance()

            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            return cal.time
        }

    val currentMonth: Date
        get() {
            val cal = Calendar.getInstance()

            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            return cal.time
        }

    // get lastest sarterday
    val currentWeek: Date
        get() {
            val cal = Calendar.getInstance()

            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek == 7) {
                return cal.time
            } else {
                cal.add(Calendar.DATE, -dayOfWeek)
            }
            return cal.time
        }

    init {
        regPatternMap[Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}$")] = "yyyy-MM-dd"
        regPatternMap[Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$")] = "yyyy-MM-dd HH:mm:ss"
        regPatternMap[Pattern.compile("^\\d{4}\\d{1,2}\\d{1,2}$")] = "yyyyMMdd"
        regPatternMap[Pattern.compile("^\\d{4}\\d{1,2}$")] = "yyyyMM"
        regPatternMap[Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$")] = "yyyy/MM/dd"
        regPatternMap[Pattern.compile("^\\d{4}年\\d{1,2}月\\d{1,2}日$")] = "yyyy年MM月dd日"
        regPatternMap[Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}$")] = "yyyy/MM/dd HH:mm:ss"
        regPatternMap[Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1}$")] =
                "yyyy/MM/dd HH:mm:ss.S"
        regPatternMap[Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1}$")] =
                "yyyy-MM-dd HH:mm:ss.S"
    }

    fun convert(timestamp: Long?): Date {
        return Date(timestamp!!)
    }

    fun now(): Date {
        return Date()
    }

    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    fun formatDate(d: Date, fmt: String): String {
        return FastDateFormat.getInstance(fmt).format(d)
    }

    fun formatYMD(d: Date): String {
        return FastDateFormat.getInstance(YMD).format(d)
    }

    fun formatYMD_HMS(d: Date): String {
        return FastDateFormat.getInstance(YMD_HMS).format(d)
    }

    fun formatDate(date: Long?, fmt: String): String {
        return FastDateFormat.getInstance(fmt).format(date)
    }

    fun format(date: Long, fmt: String): String {
        return FastDateFormat.getInstance(fmt).format(date)
    }

    fun parse(date: String, fmt: String): Date? {
        try {
            return FastDateFormat.getInstance(fmt).parse(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun parse(date: String): Date {
        try {
            for ((key, value) in regPatternMap) {
                val isMatch = key.matcher(date).matches()
                if (isMatch) {
                    return FastDateFormat.getInstance(value).parse(date)
                }
            }
        } catch (e: ParseException) {
            throw IllegalArgumentException("can't support this pattern , date is $date")
        }

        throw IllegalArgumentException("can't support this pattern , date is $date")
    }

    fun parseYMD(date: String): Date? {
        try {
            return FastDateFormat.getInstance(YMD).parse(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return null
    }

    fun parseYMD_HMS(date: String): Date {
        try {
            return FastDateFormat.getInstance(YMD_HMS).parse(date)
        } catch (e: ParseException) {
            throw IllegalArgumentException("the date pattern is error!")
        }

    }

    fun isSameDay(date: Date?, date2: Date?): Boolean {
        if (date == null || date2 == null) {
            return false
        }
        val df = FastDateFormat.getInstance(YMD)
        return df.format(date).equals(df.format(date2))
    }

    fun addMonth(date: Date, interval: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, interval)
        return calendar.time
    }

    fun addDay(date: Date, interval: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, interval)
        return calendar.time
    }

    fun addHour(date: Date, interval: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.HOUR_OF_DAY, interval)
        return calendar.time
    }

    fun addMinute(date: Date, interval: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, interval)
        return calendar.time
    }

    fun dayStart(date: Date?): Date? {
        if (date == null) {
            return null
        }
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun dayEnd(date: Date?): Date? {
        if (date == null) {
            return null
        }
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

    fun getCurrentDay(index: Int): Date {
        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, index)

        return cal.time
    }

    @JvmOverloads
    fun getCurrentDay(timestamp: Long, index: Int = 0): Date {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, index)
        return cal.time
    }

    fun getCurrentHour(index: Int): Date {
        val cal = Calendar.getInstance()

        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, index)

        return cal.time
    }

}
