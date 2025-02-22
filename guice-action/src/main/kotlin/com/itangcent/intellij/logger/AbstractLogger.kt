package com.itangcent.intellij.logger

import org.apache.commons.lang3.StringUtils

abstract class AbstractLogger : Logger {

    protected abstract fun processLog(logData: String?)

    protected open fun processLog(level: Logger.Level, msg: String) {
        try {
            val formatMsg: String = if (StringUtils.isEmpty(level.getLevelStr())) {
                msg
            } else {
                "[" + level.getLevelStr() + "]\t" + msg
            }
            processLog(formatMsg)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun log(level: Logger.Level, msg: String) {
        if (level.getLevel() < currentLogLevel().getLevel()) {
            return
        }
        processLog(level, msg)
    }

    open fun currentLogLevel(): Logger.Level {
        return Logger.BasicLevel.ALL
    }
}
