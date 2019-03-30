package com.itangcent.intellij.logger

import org.apache.commons.lang3.StringUtils

class SystemLogger : Logger {
    override fun log(level: String?, msg: String) {
        if (StringUtils.isEmpty(level)) {
            println(msg)
        } else {
            println("[$level]$msg")
        }
    }
}
