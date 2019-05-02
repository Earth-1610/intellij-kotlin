package com.itangcent.common.utils

import com.itangcent.common.function.Holder
import org.apache.commons.lang3.ArrayUtils
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Created by tangcent on 4/1/17.
 */
object TimeSpanUtils {

    private val unitMaps = HashMap<String, TimeUnit>()
    private val units = arrayOf(
        TimeUnit.NANOSECONDS,
        TimeUnit.MICROSECONDS,
        TimeUnit.MILLISECONDS,
        TimeUnit.SECONDS,
        TimeUnit.MINUTES,
        TimeUnit.HOURS,
        TimeUnit.DAYS
    )
    private val unitStrs = arrayOf("ns", "us", "ms", "s", "m", "h", "d")
    private val carries = longArrayOf(1000L, 1000L, 1000L, 60L, 60L, 24L)

    fun parse(timeSpan: String): Long? {
        if (org.apache.commons.lang3.StringUtils.isBlank(timeSpan)) {
            return 0L
        }
        val holder = Holder.of(0L)
        match(
            timeSpan,
            BiConsumer { duration, unit ->
                holder.updateData(Function { span ->
                    span!! + convert(
                        duration,
                        unit,
                        TimeUnit.MILLISECONDS
                    ).toLong()
                })
            })
        return holder.getData()
    }

    fun parse(timeSpan: String, timeUnit: TimeUnit): Long {
        return timeUnit.convert(parse(timeSpan)!!, TimeUnit.MILLISECONDS)
    }

    @JvmOverloads
    fun precisionParse(timeSpan: String, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Double? {
        if (org.apache.commons.lang3.StringUtils.isBlank(timeSpan)) {
            return 0.0
        }
        val holder = Holder.of<Double>(0.0)
        match(
            timeSpan,
            BiConsumer { duration, unit ->
                holder.updateData(Function { span ->
                    span!! + convert(
                        duration,
                        unit,
                        timeUnit
                    )
                })
            })
        return holder.getData()
    }

    fun pretty(duration: Long, unit: TimeUnit): String {
        var _duration = duration

        var index = ArrayUtils.indexOf(units, unit)
        val stringBuilder = StringBuilder()
        var carry: Long
        var rest: Long
        while (index < units.size - 1) {
            carry = _duration / carries[index]
            rest = _duration - carry * carries[index]
            if (rest > 0) {
                stringBuilder.insert(0, unitStrs[index])
                    .insert(0, rest)
            }
            if (carry == 0L) {
                break
            }
            _duration = carry
            index++
        }
        if (index == units.size - 1) {
            stringBuilder.insert(0, unitStrs[index])
                .insert(0, _duration)
        }
        return if (stringBuilder.isEmpty()) {
            "0" + unitStrs[ArrayUtils.indexOf(units, unit)]
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

    init {
        unitMaps["ns"] = TimeUnit.NANOSECONDS
        unitMaps["us"] = TimeUnit.MICROSECONDS
        unitMaps["ms"] = TimeUnit.MILLISECONDS
        unitMaps["s"] = TimeUnit.SECONDS
        unitMaps["m"] = TimeUnit.MINUTES
        unitMaps["h"] = TimeUnit.HOURS
        unitMaps["d"] = TimeUnit.DAYS
    }

    fun convert(sourceDuration: Double, sourceUnit: TimeUnit, targetUnit: TimeUnit): Double {
        var rate = targetUnit.convert(1, sourceUnit)
        if (rate > 0) {
            return sourceDuration * rate
        } else {
            rate = sourceUnit.convert(1, targetUnit)
            return sourceDuration / rate
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val strings = arrayOf("10s", "1h10s", "0.2h", "200h", "20d20h", "700000s", "0.05d")


        for (s in strings) {
            for (unit in TimeUnit.values()) {
                val spans = TimeSpanUtils.parse(s, unit)
                System.out.printf(
                    "[%s,%s]-> parse:%d\tprecision:%s\n",
                    s, unit.name, spans,
                    TimeSpanUtils.precisionParse(s, unit)
                )
                System.out.printf("[%d %s] -> %s\n", spans, unit.name, pretty(spans, unit))
            }
        }
    }
}
