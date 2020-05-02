package com.itangcent.common.utils

import java.util.*

fun Long.asDate(): Date {
    return Date(this)
}

fun Date.formatDate(fmt: String): String {
    return DateUtils.formatDate(this, fmt)
}

fun Date.formatYMD(): String {
    return DateUtils.formatYMD(this)
}

fun Date.formatYMD_HMS(): String {
    return DateUtils.formatYMD_HMS(this)
}

fun Date.isSameDay(date2: Date?): Boolean {
    return DateUtils.isSameDay(this, date2)
}

fun Date.addMonth(interval: Int): Date {
    return DateUtils.addMonth(this, interval)
}

fun Date.addDay(interval: Int): Date {
    return DateUtils.addDay(this, interval)
}

fun Date.addHour(interval: Int): Date {
    return DateUtils.addHour(this, interval)
}

fun Date.addMinute(interval: Int): Date {
    return DateUtils.addMinute(this, interval)
}

fun Date.dayStart(): Date? {
    return DateUtils.dayStart(this)
}

fun Date.dayEnd(): Date {
    return DateUtils.dayEnd(this)!!
}

