package com.itangcent.common.utils

import com.itangcent.common.concurrent.MutableHolder
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.regex.Pattern

/**
 * @author tangcent
 */
object TimeSpanUtils {

    fun parse(timeSpan: String): Long? {
        if (timeSpan.isBlank()) {
            return 0L
        }
        val holder = MutableHolder.of(0L)
        match(timeSpan) { duration, unit ->
            holder.updateData { span ->
                span!! + convert(
                    duration,
                    unit,
                    TimeUnit.MILLISECONDS
                ).toLong()
            }
        }
        return holder.value()
    }

    fun parse(timeSpan: String, timeUnit: TimeUnit): Long {
        return timeUnit.convert(parse(timeSpan)!!, TimeUnit.MILLISECONDS)
    }

    @JvmOverloads
    fun precisionParse(timeSpan: String, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Double? {
        if (timeSpan.isBlank()) {
            return 0.0
        }
        val holder = MutableHolder.of(0.0)
        match(timeSpan) { duration, unit ->
            holder.updateData { span ->
                span!! + convert(
                    duration,
                    unit,
                    timeUnit
                )
            }
        }
        return holder.value()
    }

    fun pretty(duration: Long, unit: TimeUnit): String {
        var _duration = duration

        var index = unit.asSpanUnit().ordinal
        val stringBuilder = StringBuilder()
        var carry: Long
        var rest: Long
        while (index < unitMaps.size - 1) {
            val spanUnit = TimeSpanUnit.values()[index]
            carry = _duration / spanUnit.carries
            rest = _duration - carry * spanUnit.carries
            if (rest > 0) {
                stringBuilder.insert(0, "$rest${spanUnit.shortName}")
            }
            if (carry == 0L) {
                break
            }
            _duration = carry
            index++
        }
        if (index == unitMaps.size - 1) {
            val spanUnit = TimeSpanUnit.values()[index]
            stringBuilder.insert(0, "$_duration${spanUnit.shortName}")
        }
        return if (stringBuilder.isEmpty()) {
            "0" + unit.asSpanUnit().shortName
        } else stringBuilder.toString()

    }

    private fun match(timeSpan: String, matcherConsumer: BiConsumer<Double, TimeUnit>) {
        val pattern = Pattern.compile("(\\d+(\\.\\d+)?)(\\D+)")
        val matcher = pattern.matcher(timeSpan)
        while (matcher.find()) {
            val strDuration = matcher.group(1)
            val duration = java.lang.Double.parseDouble(strDuration)
            val strUnit = matcher.group(3)
            val unit = unitMaps[strUnit] ?: throw IllegalArgumentException("unsupported unit:[$strUnit]")
            matcherConsumer.accept(duration, unit)
        }
    }

    fun convert(sourceDuration: Double, sourceUnit: TimeUnit, targetUnit: TimeUnit): Double {
        val rate = sourceUnit.asSpanUnit().ns.toDouble() / targetUnit.asSpanUnit().ns.toDouble()
        return (sourceDuration.toBigDecimal() * rate.toBigDecimal()).setScale(6, RoundingMode.HALF_UP).toDouble()
    }
}


private enum class TimeSpanUnit(val shortName: String, val carries: Long) {
    NANOSECONDS("ns", 1000L),
    MICROSECONDS("us", 1000L),
    MILLISECONDS("ms", 1000L),
    SECONDS("s", 60L),
    MINUTES("m", 60L),
    HOURS("h", 24L),
    DAYS("d", 9999),
    ;

    val timeUnit: TimeUnit = TimeUnit.valueOf(this.name)
    val ns: Long = this.timeUnit.toNanos(1)
}

private val unitMaps = TimeSpanUnit.values()
    .mapToTypedArray { it.shortName to it.timeUnit }
    .let { mapOf(*it) }

private fun TimeUnit.asSpanUnit(): TimeSpanUnit {
    return TimeSpanUnit.valueOf(this.name)
}