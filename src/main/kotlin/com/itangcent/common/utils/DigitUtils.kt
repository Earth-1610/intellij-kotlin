package com.itangcent.common.utils

/**
 * Created by tangcent on 5/25/17.
 */
object DigitUtils {
    fun mod(x: Long, m: Int): Int {
        val result = (x % m).toInt()
        return if (result >= 0) result else result + m
    }

    fun sub(high: Long, low: Long, index: Int, length: Int): Long {
        if (index < 64) {
            if (index + length <= 64) {
                return sub(high, index, length)
            } else {
                val lowLength = index + length - 64
                val highLength = length - lowLength
                return (sub(high, 64 - highLength, highLength) shl lowLength) + sub(low, 0, lowLength)
            }
        } else {
            return sub(low, index - 64, length)
        }
    }

    fun sub(l: Long, index: Int, length: Int): Long {
        return if (length == 0) {
            0
        } else (l shl index).ushr(64 - length)
        //remove pre
    }

    /**
     * reversal and take the mean
     */
    fun reversalMean(x: Long, y: Long): Long {
        var x = x
        var y = y
        x = (x shr 16) + (x shl 48)
        y = (y shr 48) + (y shl 16)
        return (x and y) + (x xor y shr 1)
    }

    /**
     * Returns val represented by the specified number of hex digits.
     */
    fun digits(l: Long, digits: Int): String {
        val hi = 1L shl digits * 4
        return java.lang.Long.toHexString(hi or (l and hi - 1)).substring(1)
    }
}
