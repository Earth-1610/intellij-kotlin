package com.itangcent.intellij.adaptor

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.VirtualFile
import com.itangcent.common.utils.invokeMethod
import com.itangcent.common.utils.invokeStaticMethod
import com.itangcent.common.utils.notNullOrBlank
import com.itangcent.intellij.context.ActionContext

object ModuleAdaptor {

    //background idea log
    private val LOG = org.apache.log4j.Logger.getLogger(ActionContext::class.java)

    fun Module.filePath(): String? {
        try {
            val path = this.invokeMethod("getModuleFilePath") as? String
            if (path.notNullOrBlank()) {
                return path
            }
        } catch (e: Exception) {
            LOG.error("NoSuchMethod: com.intellij.openapi.module.Module.getModuleFilePath")
        }
        return this.file()?.path
    }

    fun Module.file(): VirtualFile? {
        try {
            val file = this.invokeMethod("getModuleFile") as? VirtualFile
            if (file != null) {
                return file
            }
        } catch (e: Exception) {
            LOG.error("NoSuchMethod: com.intellij.openapi.module.Module.getModuleFile")
        }

        try {
            val file = ModuleUtil::class.invokeStaticMethod("suggestBaseDirectory", this) as? VirtualFile
            if (file != null) {
                return file
            }
        } catch (e: Exception) {
            LOG.error("NoSuchMethod: com.intellij.openapi.module.ModuleUtil.suggestBaseDirectory")
        }

        try {
            val file = ProjectUtil::class.invokeStaticMethod("guessModuleDir", this) as? VirtualFile
            if (file != null) {
                return file
            }
        } catch (e: Exception) {
            LOG.error("NoSuchMethod: com.intellij.ide.impl.ProjectUtil.guessModuleDir")
        }

        return null
    }
}