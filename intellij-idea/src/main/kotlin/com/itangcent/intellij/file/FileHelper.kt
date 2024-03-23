package com.itangcent.intellij.file

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.intellij.adaptor.ModuleAdaptor.filePath
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.psi.ContextSwitchListener
import com.itangcent.intellij.util.ActionUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Singleton
class FileHelper {

    @Inject
    private lateinit var logger: Logger

    @Inject
    private val contextSwitchListener: ContextSwitchListener? = null

    fun searchConfigFiles(configFileNames: List<String>): List<String>? {

        var currentPath = contextSwitchListener?.getModule()?.filePath() ?: ActionUtils.findCurrentPath()
        ?: return null

        val configFiles: ArrayList<String> = ArrayList()

        currentPath = currentPath.replace(File.separator, "/")
        while (currentPath.isNotBlank()) {
            searchConfigFileInFolder(currentPath, configFileNames).forEach { configFiles.add(it) }
            if (!currentPath.contains("/")) {
                break
            }
            currentPath = currentPath.substringBeforeLast("/")
        }

        return configFiles
    }

    private val configCache = ConcurrentHashMap<String, List<String>>()

    fun searchConfigFileInFolder(path: String, configFileNames: List<String>): List<String> =
        configCache.computeIfAbsent(path) {
            val configFiles = mutableListOf<String>()
            for (configFileName in configFileNames) {
                val configFile = File("$path/$configFileName")
                if (configFile.exists() && configFile.isFile) {
                    logger.trace("found config file:${configFile.path}")
                    configFiles.add(configFile.path);
                }
            }
            return@computeIfAbsent configFiles
        }
}