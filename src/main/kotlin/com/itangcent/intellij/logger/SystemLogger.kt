package com.itangcent.intellij.logger

import org.apache.commons.lang3.StringUtils

class SystemLogger : Logger {
    override fun log(level: Logger.Level, msg: String) {
        if (StringUtils.isEmpty(level.levelStr)) {
            println(msg)
        } else {
            println("[$level]$msg")
        }
    }
}
