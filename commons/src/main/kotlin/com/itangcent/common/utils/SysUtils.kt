package com.itangcent.common.utils


object SysUtils {

    private val NEW_LINE = System.getProperty("line.separator")

    val isWindows: Boolean
        get() {
            val OS = System.getProperty("os.name")
            return OS.toLowerCase().contains("windows")
        }

    fun newLine(): String {
        return NEW_LINE ?: "\r\n"
    }
}