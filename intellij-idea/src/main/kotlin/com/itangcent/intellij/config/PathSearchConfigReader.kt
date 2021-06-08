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

        var currentPath = contextSwitchListener?.getModule()?.filePath() ?: ActionUtils.findCurrentPath()
        ?: return null

        val configFiles: ArrayList<String> = ArrayList()

        currentPath = currentPath.replace(File.separator, "/")
        while (currentPath.isNotBlank()) {
            searchConfigFileInFolder(currentPath, configFileNames) { configFiles.add(it) }
            if (!currentPath.contains("/")) {
                break
            }
            currentPath = currentPath.substringBeforeLast("/")
        }

        return configFiles
    }

    protected fun searchConfigFileInFolder(path: String, configFileNames: List<String>, handle: (String) -> Unit) {
        for (configFileName in configFileNames) {
            val configFile = File("$path/$configFileName")
            if (configFile.exists() && configFile.isFile) {
                logger.trace("find config file:${configFile.path}")
                handle(configFile.path);
            }
        }
    }
}
