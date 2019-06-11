package com.itangcent.intellij.logger

import com.itangcent.intellij.util.Utils
import org.apache.commons.lang3.StringUtils

abstract class AbstractLogger : Logger {

    protected abstract fun processLog(logData: String?)

    override fun log(level: Logger.Level, msg: String) {
        if (level.getLevel() < currentLogLevel().getLevel()) {
            return
        }
        try {
            val formatMsg: String? = if (StringUtils.isEmpty(level.getLevelStr())) {
                    msg + Utils.newLine()
                } else {
                    "[" + level.getLevelStr() + "]\t" + msg + Utils.newLine()
                }
            processLog(formatMsg)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    open fun currentLogLevel(): Logger.Level {
        return Logger.BasicLevel.ALL
    }
}
