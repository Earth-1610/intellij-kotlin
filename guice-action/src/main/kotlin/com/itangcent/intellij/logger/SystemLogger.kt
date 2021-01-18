package com.itangcent.intellij.logger

import org.apache.commons.lang3.StringUtils

class SystemLogger : Logger {

    override fun log(level: Logger.Level, msg: String) {
        if (StringUtils.isEmpty(level.getLevelStr())) {
            LOG.info(msg)
        } else {
            LOG.info("[$level]$msg")
        }
    }

    companion object {
        private val LOG = org.apache.log4j.Logger.getLogger(SystemLogger::class.java)
    }
}
