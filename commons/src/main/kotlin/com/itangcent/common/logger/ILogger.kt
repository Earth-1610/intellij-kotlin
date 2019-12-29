package com.itangcent.common.logger

import com.itangcent.common.exception.ProcessCanceledException
import org.apache.commons.lang3.exception.ExceptionUtils


interface ILogger {
    fun log(msg: String)

    fun trace(msg: String)

    fun debug(msg: String)

    fun info(msg: String)

    fun warn(msg: String)

    fun error(msg: String)
}

fun ILogger.traceError(msg: String, e: Throwable) {
    this.error(msg)
    this.traceError(e)
}

fun ILogger.traceError(e: Throwable) {
    if (e !is ProcessCanceledException) {
        this.trace(ExceptionUtils.getStackTrace(e))
    }
}