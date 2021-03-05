package com.itangcent.intellij.logger

import org.apache.commons.lang3.StringUtils
import kotlin.reflect.KClass

class SystemLogger : Logger {

    private val delegate: org.apache.log4j.Logger

    constructor() : this(LOG)

    constructor(name: String) : this(org.apache.log4j.Logger.getLogger(name))

    constructor(clazz: Class<*>) : this(org.apache.log4j.Logger.getLogger(clazz))

    constructor(clazz: KClass<*>) : this(org.apache.log4j.Logger.getLogger(clazz.java))

    constructor(delegate: org.apache.log4j.Logger) {
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
        private val LOG = org.apache.log4j.Logger.getLogger(SystemLogger::class.java)
    }
}
