package com.itangcent.test

import com.itangcent.common.utils.IDUtils
import com.itangcent.common.utils.TimeSpanUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.concurrent.TimeUnit


/**
 * Test case of [IDUtils]
 */
class TimeSpanUtilsTest {

    @ParameterizedTest
    @CsvSource(
        "10,10ms", "3610000,1h10s", "720000,0.2h",
        "720000000,200h", "1800000000,20d20h", "700000000,700000s", "4320000,0.05d"
    )
    fun testParse(ms: Long, str: String) {
        assertEquals(ms, TimeSpanUtils.parse(str))
    }

    @ParameterizedTest
    @CsvSource(
        "0.05,180000ms", "1.1,1h6m", "0.2,0.2h",
        "200,200h", "500,20d20h", "1000,3600000s", "1.2,0.05d"
    )
    fun testPrecisionParse(ms: Double, str: String) {
        assertEquals(ms, TimeSpanUtils.precisionParse(str, TimeUnit.HOURS))
    }

    @ParameterizedTest
    @CsvSource(
        "10,10ms", "3610000,1h10s", "720000,12m",
        "720000000,8d8h", "1800000000,20d20h", "700000000,8d2h26m40s", "4320000,1h12m"
    )
    fun testPretty(ms: Long, str: String) {
        assertEquals(str, TimeSpanUtils.pretty(ms, TimeUnit.MILLISECONDS))
    }

    @ParameterizedTest
    @CsvSource(
        "720000,MILLISECONDS,0.2,HOURS",
        "6000000,MILLISECONDS,100,MINUTES",
        "0.01,DAYS,864000,MILLISECONDS",
        "3,HOURS,10800,SECONDS"
    )
    fun testConvert(sourceDuration: Double, sourceUnit: TimeUnit, targetDuration: Double, targetUnit: TimeUnit) {
        assertEquals(targetDuration, TimeSpanUtils.convert(sourceDuration, sourceUnit, targetUnit))
    }
}