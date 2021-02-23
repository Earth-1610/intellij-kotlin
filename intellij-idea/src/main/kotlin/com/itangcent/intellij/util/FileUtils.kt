package com.itangcent.intellij.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.itangcent.common.utils.forceMkdirParent
import java.io.File
import java.util.*

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
            localFile.forceMkdirParent()
            localFile.createNewFile()
        }
        localFile.writeBytes(content)
    }

    fun traversal(
        psiDirectory: PsiDirectory,
        fileFilter: FileFilter,
        fileHandle: (PsiFile) -> Unit
    ) {

        val dirStack: Stack<PsiDirectory> = Stack()
        var dir: PsiDirectory? = psiDirectory
        while (dir != null) {
            dir.files.filter { fileFilter(it) }
                .forEach { fileHandle(it) }

            for (subdirectory in dir.subdirectories) {
                dirStack.push(subdirectory)
            }
            if (dirStack.isEmpty()) break
            dir = dirStack.pop()
        }
    }
}

typealias FileFilter = (PsiFile) -> Boolean

typealias DirFilter = (PsiDirectory, (Boolean) -> Unit) -> Unit