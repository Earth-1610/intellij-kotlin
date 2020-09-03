package com.itangcent.intellij.config

import com.intellij.util.containers.isNullOrEmpty
import java.util.*

abstract class AutoSearchConfigReader : PathSearchConfigReader() {

    abstract fun configFileNames(): List<String>

    override fun findConfigFiles(): List<String>? {
        val configFiles = searchConfigFiles(configFileNames())
        if (configFiles.isNullOrEmpty()) {
            logger?.trace("No config be found")
        } else {
            return configFiles
        }
        return Collections.emptyList()
    }
}
