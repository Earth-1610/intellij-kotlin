package com.itangcent.mock

import com.google.inject.Singleton
import com.itangcent.intellij.logger.Level
import com.itangcent.intellij.logger.Logger

@Singleton
class PrintLogger : Logger {
    override fun log(level: Level, msg: String) {
        println("[${level.name}]\t$msg")
    }
}