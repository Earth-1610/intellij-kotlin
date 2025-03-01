package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.intellij.file.FileHelper

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

    private fun findConfigFiles(): List<String>? = fileHelper.searchConfigFiles(configFileNames())
}
