package com.itangcent.common.logger

import com.itangcent.common.spi.SpiUtils

open class Log {

    //background idea log
    protected val LOG: ILogger = SpiUtils.loadService(ILoggerProvider::class)?.getLogger(this::class) ?: Log

    companion object : ILogger {
        override fun log(msg: String) {
            print(msg)
        }

        override fun trace(msg: String) {
            print(msg)
        }

        override fun debug(msg: String) {
            print(msg)
        }

        override fun info(msg: String) {
            print(msg)
        }

        override fun warn(msg: String) {
            print(msg)
        }

        override fun error(msg: String) {
            print(msg)
        }
    }
}