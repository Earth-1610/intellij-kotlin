package com.itangcent.intellij.logger

import org.apache.commons.lang3.StringUtils
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

    override fun log(level: Logger.Level, msg: String) {
        if (StringUtils.isEmpty(level.getLevelStr())) {
            delegate.info(msg)
        } else {
            delegate.info("[$level]$msg")
        }
    }

    companion object {
        private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(SystemLogger::class.java)
    }
}
