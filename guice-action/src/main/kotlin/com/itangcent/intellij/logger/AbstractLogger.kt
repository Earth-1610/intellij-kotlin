package com.itangcent.intellij.logger

@Deprecated("Directly use Logger")
abstract class AbstractLogger : Logger {

    protected abstract fun processLog(level: Level, logData: String?)

    override fun log(level: Level, msg: String) {
        processLog(level, "[${level.name}]\t$msg")
    }
}
