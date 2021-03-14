package com.itangcent.common.files


import com.itangcent.common.utils.FileUtils
import java.io.File
import java.io.IOException

/**
 * Created by tangcent on 6/16/17.
 */
class FileWrap(private val root: String, var file: File) {

    fun root(): String {
        return root
    }

    fun content(): String {
        return FileUtils.read(file) ?: ""
    }

    fun path(): String {
        return file.path
    }

    fun parent(): String {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileWrap

        if (root != other.root) return false
        if (file != other.file) return false

        return true
    }

    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + file.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileWrap(root='$root', file=$file)"
    }
}
