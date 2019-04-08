package com.itangcent.intellij.file

import com.google.inject.Inject
import com.google.inject.name.Named
import com.itangcent.intellij.logger.Logger
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.File

open abstract class AbstractLocalFileRepository : LocalFileRepository {
    @Inject
    protected val logger: Logger? = null

    abstract fun basePath(): String

    @Inject(optional = true)
    @Named("plugin.name")
    protected val pluginName: String = "shadow"

    override fun getFile(path: String): File {
        val repositoryFile = "${basePath()}/.$pluginName/$path"
        val file = File(repositoryFile)

        if (!file.exists()) {
            try {
                FileUtils.forceMkdirParent(file)
                if (!file.createNewFile()) {
                    logger!!.error("error to create new file:$repositoryFile")
                }
            } catch (e: Throwable) {
                logger?.error("error to create new file:$repositoryFile\n${ExceptionUtils.getStackTrace(e)}")
                throw RuntimeException(e)
            }
        }
        return file
    }
}