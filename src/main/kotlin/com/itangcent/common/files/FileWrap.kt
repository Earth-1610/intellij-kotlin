package com.itangcent.common.files


import com.itangcent.common.utils.FileUtils
import java.io.File
import java.io.IOException

/**
 * Created by tangcent on 6/16/17.
 */
class FileWrap(private val root: String, var file: File) {

    fun content(): String {
        return FileUtils.read(file)
    }

    fun path(): String {
        return file.parent
    }

    fun name(): String {
        return file.name
    }

    fun write(content: String) {
        try {
            FileUtils.write(file, content)
        } catch (e: IOException) {
            System.out.printf("error to write file:$file")
        }

    }

    fun rename(name: String): FileWrap {
        this.file = FileUtils.renameFile(file, name)
        return this
    }
}
