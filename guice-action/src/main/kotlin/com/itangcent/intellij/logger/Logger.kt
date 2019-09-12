package com.itangcent.intellij.logger

import com.google.inject.ImplementedBy
import com.google.inject.Singleton


@ImplementedBy(ConsoleRunnerLogger::class)
interface Logger {
    fun log(msg: String) {
        log(BasicLevel.ALL, msg)
    }

    fun log(level: Level, msg: String)

    fun trace(msg: String) {
        log(BasicLevel.TRACE, msg)
    }

    fun debug(msg: String) {
        log(BasicLevel.DEBUG, msg)
    }

    fun info(msg: String) {
        log(BasicLevel.INFO, msg)
    }

    fun warn(msg: String) {
        log(BasicLevel.WARN, msg)
    }

    fun error(msg: String) {
        log(BasicLevel.ERROR, msg)
    }

    interface Level {

        fun getLevelStr(): String

        fun getLevel(): Int
    }

    /**
     * Like standard log levels,
     * But only a partial available log level is provided,
     * And no OFF level,It means that plugin log cannot be turned OFF
     */
    enum class BasicLevel : Level {
        ALL("", -1),
        TRACE("TRACE", 100),
        DEBUG("DEBUG", 200),
        INFO("INFO", 300),
        WARN("WARN", 400),
        ERROR("ERROR", 500)
        ;

        private val levelStr: String
        private val level: Int

        constructor(levelStr: String, level: Int) {
            this.levelStr = levelStr
            this.level = level
        }

        override fun getLevelStr(): String {
            return levelStr
        }

        override fun getLevel(): Int {
            return level
        }

        override fun toString(): String {
            return "[$levelStr $level]"
        }

        companion object {

            fun toLevel(levelStr: String): Level {
                return toLevel(levelStr, ALL)
            }

            fun toLevel(level: Int): Level {
                return toLevel(level, ALL)
            }

            fun toLevel(level: Int, defaultLevel: Level): Level {
                return when (level) {
                    ALL.level -> ALL
                    TRACE.level -> TRACE
                    DEBUG.level -> DEBUG
                    INFO.level -> INFO
                    WARN.level -> WARN
                    ERROR.level -> ERROR
                    else -> defaultLevel
                }
            }

            fun toLevel(levelStr: String?, defaultLevel: Level): Level {
                return when (levelStr?.toUpperCase()) {
                    null -> defaultLevel
                    "ALL" -> ALL
                    "DEBUG" -> DEBUG
                    "INFO" -> INFO
                    "WARN" -> WARN
                    "ERROR" -> ERROR
                    "TRACE" -> TRACE
                    else -> defaultLevel
                }
            }
        }
    }
}
