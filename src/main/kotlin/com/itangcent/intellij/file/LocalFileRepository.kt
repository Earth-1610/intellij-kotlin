package com.itangcent.intellij.file

import java.io.File

interface LocalFileRepository {
    fun getFile(path: String): File?

    fun deleteFile(path: String)

    fun getOrCreateFile(path: String): File
}