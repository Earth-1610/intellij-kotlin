package com.itangcent.intellij.logger

import com.itangcent.intellij.util.Utils
import org.apache.commons.lang3.StringUtils

abstract class AbstractLogger : Logger {

    protected abstract fun processLog(logData: String?)

    override fun log(level: String?, msg: String) {
        try {
            val formatMsg: String? =
                if (StringUtils.isEmpty(level)) {
                    msg + Utils.newLine()
                } else {
                    "[" + level + "]\t" + msg + Utils.newLine()
                }
            processLog(formatMsg)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }
}
