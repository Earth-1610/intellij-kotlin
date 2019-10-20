package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.util.ActionUtils
import java.io.File

abstract class PathSearchConfigReader : AbstractConfigReader() {

    protected fun searchConfigFiles(configFileName: String): List<String>? {

        val configFiles: ArrayList<String> = ArrayList()

        var currentPath = ActionUtils.findCurrentPath()

        while (!currentPath.isNullOrBlank()) {
            val path = "$currentPath${File.separator}$configFileName"

            val configFile = File(path)
            if (configFile.exists() && configFile.isFile) {
                logger?.trace("find config file:$path")
                configFiles.add(path)
            }
            if (currentPath.isNullOrBlank() || !currentPath.contains(File.separator)) {
                break
            }
            currentPath = currentPath.substringBeforeLast(File.separator)
        }

        return configFiles
    }
}
