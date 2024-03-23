package com.itangcent.intellij.config

import com.google.inject.Inject
import com.intellij.util.containers.isNullOrEmpty
import com.itangcent.intellij.file.FileHelper
import java.util.*

@Deprecated("use LocalFileSearchConfigProvider")
abstract class AutoSearchConfigReader : BaseConfigReader() {

    @Inject
    protected lateinit var fileHelper: FileHelper

    abstract fun configFileNames(): List<String>

    fun loadConfigInfo() {
        val configFiles = findConfigFiles() ?: return
        configFiles.forEach { path ->
            loadConfigFile(path)
        }
    }

    private fun findConfigFiles(): List<String>? {
        val configFiles = fileHelper.searchConfigFiles(configFileNames())
        if (configFiles.isNullOrEmpty()) {
            logger.trace("No config be found")
        } else {
            return configFiles
        }
        return Collections.emptyList()
    }
}
