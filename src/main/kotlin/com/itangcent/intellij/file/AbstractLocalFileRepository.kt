package com.itangcent.intellij.file

import com.google.inject.Inject
import com.google.inject.name.Named
import com.itangcent.intellij.logger.Logger
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.File

abstract class AbstractLocalFileRepository : LocalFileRepository {
    @Inject
    protected val logger: Logger? = null

    abstract fun basePath(): String

    @Inject(optional = true)
    @Named("plugin.name")
    protected val pluginName: String = "shadow"

    private fun fileOf(path: String): File {
        val repositoryFile = "${basePath()}/.$pluginName/$path"
        return File(repositoryFile)
    }

    override fun getFile(path: String): File? {
        val file = fileOf(path)

        if (file.exists()) {
            return file
        }
        return null
    }

    override fun getOrCreateFile(path: String): File {
        val file = fileOf(path)

        if (!file.exists()) {
            try {
                FileUtils.forceMkdirParent(file)
                if (!file.createNewFile()) {
                    logger!!.error("error to create new file:${file.path}")
                }
            } catch (e: Throwable) {
                logger?.error("error to create new file:${file.path}")
                logger?.trace(ExceptionUtils.getStackTrace(e))
                throw RuntimeException(e)
            }
        }
        return file
    }

    override fun deleteFile(path: String) {
        val file = fileOf(path)

        if (file.exists()) {
            FileUtils.forceDelete(file)
        }
    }
}