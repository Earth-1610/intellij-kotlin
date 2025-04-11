package com.itangcent.intellij.logger

import kotlin.reflect.KClass

class SystemLogger : Logger {

    private val delegate: com.intellij.openapi.diagnostic.Logger

    constructor() : this(LOG)

    constructor(name: String) : this(com.intellij.openapi.diagnostic.Logger.getInstance(name))

    constructor(clazz: Class<*>) : this(com.intellij.openapi.diagnostic.Logger.getInstance(clazz))

    constructor(clazz: KClass<*>) : this(com.intellij.openapi.diagnostic.Logger.getInstance(clazz.java))

    constructor(delegate: com.intellij.openapi.diagnostic.Logger) {
        this.delegate = delegate
    }

    override fun log(level: Level, msg: String) {
        when (level) {
            Level.TRACE -> delegate.trace(msg)
            Level.DEBUG -> delegate.debug(msg)
            Level.INFO -> delegate.info(msg)
            Level.WARN -> delegate.warn(msg)
            Level.ERROR -> delegate.warn(msg)
        }
    }

    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(SystemLogger::class.java)
    }
}
