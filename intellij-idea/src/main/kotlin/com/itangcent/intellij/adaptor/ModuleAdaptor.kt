package com.itangcent.intellij.adaptor

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.itangcent.common.logger.Log
import com.itangcent.common.spi.SafeProxyBean
import com.itangcent.common.spi.createProxy
import java.io.File

object ModuleAdaptor : Log() {

    @Suppress("UNCHECKED_CAST")
    private val MODULE_FILE_PATH_GETTER: ModuleFilePathGetter by lazy {
        val getters = ArrayList<ModuleFilePathGetter>()
        try {
            val projectUtilKClass = Class.forName("com.intellij.openapi.project.ProjectUtilKt")
            projectUtilKClass.methods
                .firstOrNull { it.name == "guessModuleDir" && it.parameterCount == 1 }
                ?.let { method ->
                    getters.add(object : ModuleFilePathGetter {
                        override fun getFile(module: Module): String? {
                            return (method.invoke(null, module) as? VirtualFile)?.path
                        }
                    })
                }
        } catch (e: Throwable) {
            LOG.warn("NoSuchMethod: com.intellij.openapi.project.ProjectUtil.guessModuleDir")
        }

        try {
            val projectUtilKClass = Class.forName("com.intellij.openapi.project.ProjectUtil")
            projectUtilKClass.methods
                .firstOrNull { it.name == "guessModuleDir" && it.parameterCount == 1 }
                ?.let { method ->
                    getters.add(object : ModuleFilePathGetter {
                        override fun getFile(module: Module): String? {
                            return (method.invoke(null, module) as? VirtualFile)?.path
                        }
                    })
                }
        } catch (e: Throwable) {
            LOG.warn("NoSuchMethod: com.intellij.openapi.project.ProjectUtil.guessModuleDir")
        }

        try {
            val moduleUtilKClass = Class.forName("com.intellij.openapi.module.ModuleUtil")
            moduleUtilKClass.methods
                .firstOrNull { it.name == "getModuleDirPath" && it.parameterCount == 0 }
                ?.let { method ->
                    getters.add(object : ModuleFilePathGetter {
                        override fun getFile(module: Module): String? {
                            return method.invoke(null, module) as? String
                        }
                    })
                }
        } catch (e: Throwable) {
            LOG.warn("NoSuchMethod: com.intellij.openapi.module.ModuleUtil.getModuleDirPath")
        }

        try {
            val moduleKClass = Module::class.java
            moduleKClass.methods
                .firstOrNull { it.name == "getModuleFilePath" && it.parameterCount == 0 }
                ?.let { method ->
                    getters.add(object : ModuleFilePathGetter {
                        override fun getFile(module: Module): String? {
                            return method.invoke(module) as? String
                        }
                    })
                }
        } catch (e: Throwable) {
            LOG.warn("NoSuchMethod: com.intellij.openapi.module.Module.getModuleFilePath")
        }

        try {
            val moduleKClass = Module::class.java
            moduleKClass.methods
                .firstOrNull { it.name == "getModuleFile" && it.parameterCount == 0 }
                ?.let { method ->
                    getters.add(object : ModuleFilePathGetter {
                        override fun getFile(module: Module): String? {
                            return (method.invoke(module) as? VirtualFile)?.path
                        }
                    })
                }
        } catch (e: Throwable) {
            LOG.warn("NoSuchMethod: com.intellij.openapi.module.Module.getModuleFile")
        }

        return@lazy SafeProxyBean(getters.toTypedArray()).createProxy(ModuleFilePathGetter::class)
    }

    fun Module.filePath(): String? {
        val filePath = MODULE_FILE_PATH_GETTER.getFile(this) ?: return null
        val file = File(filePath)
        return if (file.exists()) {
            if (file.isFile) file.parent else filePath
        } else {
            if (file.name.contains(".")) file.parent else filePath
        }
    }
}

@FunctionalInterface
private interface ModuleFilePathGetter {
    fun getFile(module: Module): String?
}