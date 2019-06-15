package com.itangcent.intellij.config

import com.google.inject.Inject
import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.intellij.config.PathSearchConfigReader
import com.itangcent.intellij.logger.Logger
import java.util.*

abstract class AutoSearchConfigReader : PathSearchConfigReader() {

    @Inject(optional = true)
    private val logger: Logger? = null

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
