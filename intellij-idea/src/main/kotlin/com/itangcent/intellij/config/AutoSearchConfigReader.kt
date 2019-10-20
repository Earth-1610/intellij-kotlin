package com.itangcent.intellij.config

import com.intellij.util.containers.isNullOrEmpty
import java.util.*

abstract class AutoSearchConfigReader : PathSearchConfigReader() {

    abstract fun configFileNames(): List<String>

    override fun findConfigFiles(): List<String>? {
        configFileNames().forEach { configFileName ->
            val configFiles = searchConfigFiles(configFileName)
            if (configFiles.isNullOrEmpty()) {
                logger?.trace("No config [$configFileName] be found")
            } else {
                return configFiles
            }
        }
        return Collections.emptyList()
    }
}
