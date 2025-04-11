package com.itangcent.intellij.logger

import com.google.inject.ImplementedBy


@ImplementedBy(IdeaConsoleLogger::class)
interface Logger : com.itangcent.common.logger.ILogger {
    override fun log(msg: String) {
        log(com.itangcent.intellij.logger.Level.INFO, msg)
    }

    @Deprecated("Use log(com.itangcent.intellij.logger.Level,String) instead")
    fun log(level: Level, msg: String) {
        log(com.itangcent.intellij.logger.Level.INFO, msg)
    }

    fun log(level: com.itangcent.intellij.logger.Level, msg: String)

    override fun trace(msg: String) {
        log(com.itangcent.intellij.logger.Level.TRACE, msg)
    }

    override fun debug(msg: String) {
        log(com.itangcent.intellij.logger.Level.DEBUG, msg)
    }

    override fun info(msg: String) {
        log(com.itangcent.intellij.logger.Level.INFO, msg)
    }

    override fun warn(msg: String) {
        log(com.itangcent.intellij.logger.Level.WARN, msg)
    }

    override fun error(msg: String) {
        log(com.itangcent.intellij.logger.Level.ERROR, msg)
    }

    @Deprecated("Use com.itangcent.intellij.logger.Level instead")
    interface Level {

        fun getLevelStr(): String

        fun getLevel(): Int
    }

    /**
     * Like standard log levels,
     * But only a partial available log level is provided,
     * And no OFF level,It means that plugin log cannot be turned OFF
     */
    @Deprecated("Use com.itangcent.intellij.logger.Level instead")
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
                return toLevel(
                    levelStr,
                    ALL
                )
            }

            fun toLevel(level: Int): Level {
                return toLevel(
                    level,
                    ALL
                )
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
                return when (levelStr?.uppercase()) {
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


enum class Level {
    TRACE(100),
    DEBUG(200),
    INFO(300),
    WARN(400),
    ERROR(500);

    val level: Int

    constructor(level: Int) {
        this.level = level
    }
}