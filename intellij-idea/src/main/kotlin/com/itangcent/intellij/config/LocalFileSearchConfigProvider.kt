package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.intellij.config.resource.ResourceResolver
import com.itangcent.intellij.file.FileHelper
import com.itangcent.intellij.logger.Logger

abstract class LocalFileSearchConfigProvider : ConfigProvider {
    override fun loadConfig(): Sequence<ConfigContent> {
        val configFiles = findConfigFiles() ?: return emptySequence()
        return configFiles.asSequence()
            .map { resourceResolver.resolve(it) }
            .map { ConfigContent(it) }
    }

    @Inject
    protected lateinit var fileHelper: FileHelper

    @Inject
    protected lateinit var logger: Logger

    @Inject
    protected lateinit var resourceResolver: ResourceResolver

    abstract fun configFileNames(): List<String>

    private fun findConfigFiles(): List<String>? = fileHelper.searchConfigFiles(configFileNames())
}