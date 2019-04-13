package com.itangcent.intellij.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File

object FileUtils {

    fun getLastModified(psiFile: PsiFile): Long? {
        val lastModified = psiFile.modificationStamp
        if (lastModified > 0) return lastModified
        val path = ActionUtils.findCurrentPath(psiFile) ?: return null
        val file = File(path)
        if (file.exists()) {
            return file.lastModified()
        }
        return null
    }

    fun forceSave(file: VirtualFile, content: ByteArray) {
        forceSave(file.path, content)
    }

    fun forceSave(filePath: String, content: ByteArray) {
        val localFile = File(filePath)
        if (!localFile.exists()) {
            org.apache.commons.io.FileUtils.forceMkdirParent(localFile)
            localFile.createNewFile()
        }
        org.apache.commons.io.FileUtils.writeByteArrayToFile(localFile, content)
    }

}