package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.intellij.adaptor.ModuleAdaptor.filePath
import com.itangcent.intellij.psi.ContextSwitchListener
import com.itangcent.intellij.util.ActionUtils
import java.io.File

abstract class PathSearchConfigReader : AbstractConfigReader() {

    @Inject
    protected val contextSwitchListener: ContextSwitchListener? = null

    protected fun searchConfigFiles(configFileNames: List<String>): List<String>? {

        val configFiles: ArrayList<String> = ArrayList()

        var currentPath = contextSwitchListener?.getModule()?.filePath() ?: ActionUtils.findCurrentPath()

        while (!currentPath.isNullOrBlank()) {
            searchConfigFileInFolder(currentPath, configFileNames) { configFiles.add(it) }
            if (currentPath.isNullOrBlank() || !currentPath.contains(File.separator)) {
                break
            }
            currentPath = currentPath.substringBeforeLast(File.separator)
        }

        return configFiles
    }

    protected fun searchConfigFileInFolder(path: String, configFileNames: List<String>, handle: (String) -> Unit) {
        for (configFileName in configFileNames) {
            val configFile = File("$path${File.separator}$configFileName")
            if (configFile.exists() && configFile.isFile) {
                logger?.trace("find config file:$path")
                handle(configFile.path);
            }
        }
    }
}
