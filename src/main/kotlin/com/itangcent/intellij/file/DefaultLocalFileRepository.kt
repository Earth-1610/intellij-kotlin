package com.itangcent.intellij.file

import com.google.inject.Inject
import com.itangcent.common.utils.SystemUtils
import com.itangcent.intellij.logger.Logger
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.File

class DefaultLocalFileRepository : LocalFileRepository {

    @Inject
    private val logger: Logger? = null

    override fun getFile(path: String): File {
        var home = SystemUtils.userHome
        if (home.endsWith("/")) {
            home = home.substring(0, home.length - 1)
        }
        val repositoryFile = "$home/.tm/$path"
        val file = File(repositoryFile)

        if (!file.exists()) {
            try {
                FileUtils.forceMkdirParent(file)
                if (!file.createNewFile()) {
                    logger!!.error("error to create new setting file:$repositoryFile")
                }
            } catch (e: Throwable) {
                logger?.error("error to create new setting file:$repositoryFile\n${ExceptionUtils.getStackTrace(e)}")
                throw RuntimeException(e)
            }
        }
        return file
    }
}