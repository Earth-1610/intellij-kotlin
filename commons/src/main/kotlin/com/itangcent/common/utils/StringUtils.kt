package com.itangcent.common.utils

/**
 * @author tangcent
 */
object StringUtils {

    fun firstCharacterIndex(charSequence: CharSequence): Int {
        return (0 until charSequence.length).firstOrNull { charSequence[it] > ' ' } ?: -1
    }

    fun camelToUnderline(param: String?): String {
        if (param == null || "" == param.trim { it <= ' ' }) {
            return ""
        }
        val len = param.length
        val sb = StringBuilder(len)
        sb.append(Character.toLowerCase(param[0]))
        (1 until len).map { param[it] }
            .forEach {
                when {
                    Character.isUpperCase(it) -> {
                        sb.append(UNDERLINE)
                        sb.append(Character.toLowerCase(it))
                    }
                    else -> sb.append(it)
                }
            }
        return sb.toString()
    }

    fun underlineToCamel(param: String?): String {
        if (param == null || "" == param.trim { it <= ' ' }) {
            return ""
        }
        val len = param.length
        val sb = StringBuilder(len)
        var i = 0
        while (i < len) {
            val c = param[i]
            if (c == UNDERLINE) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param[i]))
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    val UNDERLINE = '_'
}


/**
 * Returns `true` if the contents of this string is equal to the word "true" or "1",
 * ignoring case, and `false` otherwise.
 */
fun String.toBool(defaultValue: Boolean = false): Boolean {
    if (this.isNullOrBlank()) return defaultValue

    return try {
        this.toBoolean() || this == "1"
    } catch (e: Exception) {
        defaultValue
    }
}