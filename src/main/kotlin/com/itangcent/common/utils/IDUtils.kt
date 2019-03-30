package com.itangcent.common.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by tangcent on 4/27/17.
 */
object IDUtils {

    private val lowSChars = "0123456789abcdefghijklmnopqrstuvwxyz"

    private val digitChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    /**
     * uuid:no bar
     */
    fun UUID(): String {
        return UUID2Tidy(UUID.randomUUID()).toLowerCase()
    }

    private fun UUID2Tidy(uuid: UUID): String {
        val mostSigBits = uuid.mostSignificantBits
        val leastSigBits = uuid.leastSignificantBits

        return DigitUtils.digits(mostSigBits shr 32, 8) +
                DigitUtils.digits(mostSigBits shr 16, 4) +
                DigitUtils.digits(mostSigBits, 4) +
                DigitUtils.digits(leastSigBits shr 48, 4) +
                DigitUtils.digits(leastSigBits, 12)
    }

    /**
     * uuid压缩至16位短id
     */
    fun shortUUID(): String {
        val uuid = UUID.randomUUID()
        val meanSign = DigitUtils.reversalMean(uuid.mostSignificantBits, uuid.leastSignificantBits)
        return DigitUtils.digits(meanSign shr 32, 8) + DigitUtils.digits(meanSign, 8)
    }

    /**
     * 生成一个简单的id
     *
     * @param length      -id长度
     * @param supportCase -区分大小写
     * @return -liteId
     * @see .liteId
     */
    @JvmOverloads
    fun liteId(length: Int, supportCase: Boolean = true): String {
        val chars = if (supportCase) digitChars else lowSChars

        val unit = 128 / (32 - Integer.numberOfLeadingZeros(chars.length - 1))

        if (length > unit) {
            val group = (length - 1) / unit
            val groupLength = length / (group + 1)

            val sb = StringBuilder()
            for (i in 0 until group) {
                sb.append(buildLiteId(groupLength, chars))
            }
            sb.append(buildLiteId(length - group * groupLength, chars))
            return sb.toString()
        } else {
            return buildLiteId(length, chars)
        }
    }

    private fun buildLiteId(length: Int, chars: String): String {
        val uuid = UUID.randomUUID()
        val high = uuid.mostSignificantBits
        val low = uuid.leastSignificantBits

        val offset = 128 / length
        val dcLength = chars.length
        val sb = StringBuilder()
        for (i in 0 until length) {
            val v = DigitUtils.sub(high, low, offset * i, offset)
            sb.append(chars[DigitUtils.mod(v, dcLength)])
        }
        return sb.toString()
    }

    fun timeId(): String {
        //第一部分是当前时间串-17位
        val SDF = SimpleDateFormat("YYYYMMddHHmmssSSS")
        val timed = SDF.format(Date())
        val tail = java.lang.Long.parseLong(timed.substring(16))
        val uuid = UUID.randomUUID()
        val meanSign = (tail shl 60) + DigitUtils.reversalMean(uuid.mostSignificantBits, uuid.leastSignificantBits)
        return timed.substring(0, 16) + (DigitUtils.digits(meanSign.ushr(32), 8) + DigitUtils.digits(meanSign, 8))
    }

}
