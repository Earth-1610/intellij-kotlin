package com.itangcent.intellij.logger

interface Logger {
    fun log(msg: String) {
        log(Level.ALL, msg)
    }

    fun log(level: Level, msg: String)

    fun trace(msg: String) {
        log(Level.TRACE, msg)
    }

    fun debug(msg: String) {
        log(Level.DEBUG, msg)
    }

    fun info(msg: String) {
        log(Level.INFO, msg)
    }

    fun warn(msg: String) {
        log(Level.WARN, msg)
    }

    fun error(msg: String) {
        log(Level.ERROR, msg)
    }

    /**
     * Like standard log levels,
     * But only a partial available log level is provided,
     * And no OFF level,It means that plugin log cannot be turned OFF
     */
    enum class Level {
        ALL("", -1),
        TRACE("TRACE", 1),
        DEBUG("DEBUG", 2),
        INFO("INFO", 3),
        WARN("WARN", 4),
        ERROR("ERROR", 5)
        ;

        val levelStr: String
        val level: Int

        constructor(levelStr: String, level: Int) {
            this.levelStr = levelStr
            this.level = level
        }

        override fun toString(): String {
            return "[$levelStr $level]"
        }

        fun toLevel(levelStr: String): Level {
            return toLevel(levelStr, DEBUG)
        }

        fun toLevel(level: Int): Level {
            return toLevel(level, DEBUG)
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
            val s = levelStr?.toUpperCase()
            return when (s) {
                null -> defaultLevel
                "ALL" -> ALL
                "DEBUG" -> DEBUG
                "INFO" -> INFO
                "WARN" -> WARN
                "ERROR" -> ERROR
                "TRACE" -> TRACE
                "İNFO" -> INFO
                else -> defaultLevel
            }
        }
    }
}
