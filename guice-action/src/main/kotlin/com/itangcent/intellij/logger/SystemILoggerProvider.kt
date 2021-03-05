package com.itangcent.intellij.logger

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.ILoggerProvider
import kotlin.reflect.KClass

class SystemILoggerProvider : ILoggerProvider {

    override fun getLogger(name: String): ILogger {
        return SystemLogger(name)
    }

    override fun getLogger(clazz: KClass<*>): ILogger {
        return SystemLogger(clazz)
    }

    override fun getLogger(clazz: Class<*>): ILogger {
        return SystemLogger(clazz)
    }
}