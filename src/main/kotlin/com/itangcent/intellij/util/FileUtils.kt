package com.itangcent.intellij.util

import com.intellij.openapi.vfs.VirtualFile
import java.io.File

object FileUtils {

    fun forceSave(file: VirtualFile, content: ByteArray) {
        forceSave(file.path, content)
    }

    fun forceSave(filePaht: String, content: ByteArray) {
        val localFile = File(filePaht)
        if (!localFile.exists()) {
            org.apache.commons.io.FileUtils.forceMkdirParent(localFile)
            localFile.createNewFile()
        }
        org.apache.commons.io.FileUtils.writeByteArrayToFile(localFile, content)
    }

}