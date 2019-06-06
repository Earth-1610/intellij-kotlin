package com.itangcent.common.utils

import com.itangcent.common.files.DefaultFileTraveler
import com.itangcent.common.files.FileHandle
import com.itangcent.common.files.FileTraveler
import org.apache.commons.lang3.StringUtils
import java.io.*
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern

object FileUtils {

    @Throws(IOException::class)
    fun search(localPath: String, filePattern: String): List<File> {
        var localPath = localPath
        var filePattern = filePattern
        if (StringUtils.endsWith(localPath, "/")) {
            localPath = localPath.substring(0, localPath.length - 1)
        }
        val fileTraveler: FileTraveler = DefaultFileTraveler(localPath)

        if (StringUtils.isNotEmpty(filePattern)) {
            if (StringUtils.startsWith(filePattern, "./")) {
                filePattern = "$localPath${filePattern.substring(1)}"
            } else if (StringUtils.startsWith(filePattern, "~/")) {
                val userHome = System.getProperty("user.home")
                filePattern = "$userHome${filePattern.substring(1)}"
            }
            filePattern = formatPattern(filePattern)
            val pattern = Pattern.compile("^$filePattern$")

            fileTraveler.filter(com.itangcent.common.files.FileFilter.filterFile {
                pattern.matcher(it.path()).matches()
            })
        }
        val files = ArrayList<File>()
        fileTraveler.onFile(FileHandle.collectFiles(files) { it.file })
        fileTraveler.travel()
        return files
    }

    /**
     * For now, it's just a simple regularization format.
     */
    private fun formatPattern(filePattern: String): String {
        val sb = StringBuilder()
        for (ch in filePattern.toCharArray()) {
            when (ch) {
                '*' -> sb.append(".*")
                '.' -> sb.append("\\.")
                '\\' -> sb.append("\\\\")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun read(file: File): String {
        val sb = StringBuilder()
        try {
            FileInputStream(file).use { `in` ->
                BufferedReader(InputStreamReader(`in`, "utf-8")).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        sb.append("\r\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (ignored: Exception) {
        }

        return sb.toString()
    }

    @Throws(IOException::class)
    fun write(file: File, content: String) {
        val out = FileOutputStream(file)
        out.use {
            out.write(content.toByteArray())
        }
    }

    fun renameFile(path: String, oldname: String, newname: String) {
        if (oldname != newname) {
            //Renaming is necessary if the new file name is different from the previous file name
            val oldfile = File("$path/$oldname")
            if (!oldfile.exists()) {
                return //The rename file does not exist
            }
            val newfile = File("$path/$newname")

            if (newfile.exists()) {
                if (!newfile.delete()) {
                    throw IllegalStateException("failed to rename [" + file.name + "] to [" + newName + "]!")
                }
            }

            oldfile.renameTo(newfile)
        }
    }

    fun renameFile(file: File, newName: String): File {
        if (file.name != newName) {
            //Renaming is necessary if the new file name is different from the previous file name
            val newPath = file.parent + "/" + newName
            doRenameFile(file, newPath)
            return File(newPath)
        }
        return file
    }

    fun move(file: File, newPath: String) {
        var newPath = newPath
        if (file.parent != newPath) {
            newPath = newPath + "/" + file.name
            doRenameFile(file, newPath)
        }
    }

    private fun doRenameFile(file: File, newName: String) {
        val newFile = File(newName)
        if (newFile.exists()) {
            if (!newFile.delete()) {
                throw IllegalStateException("failed to rename [" + file.name + "] to [" + newName + "]!")
            }
        }
        if (!file.renameTo(newFile)) {
            throw IllegalStateException("failed to rename [" + file.name + "] to [" + newName + "]!")
        }
    }

    fun remove(file: File) {
        file.delete()
    }

    fun isEmptyDir(file: File): Boolean {
        if (file.isDirectory) {
            val files = file.list()
            return files == null || files.isEmpty()
        } else
            return false
    }

    fun cleanEmptyDir(path: String) {
        val file = File(path)
        if (!file.exists()) {
            return
        }
        val errorFiles = Stack<File>()
        removeEmptyDir(file, Consumer { errorFiles.push(it) })
        while (!errorFiles.empty()) {
            errorFiles.pop().delete()
        }
    }

    fun removeEmptyDir(file: File): Boolean {
        return removeEmptyDir(file, null)
    }

    fun removeEmptyDir(file: File, onDeleteFailed: Consumer<File>?): Boolean {
        var flag = true
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (subFile in files) {
                    if (subFile.isFile || !removeEmptyDir(subFile)) {
                        flag = false
                    }
                }
            }
            if (flag) {
                if (!file.delete() && onDeleteFailed != null) {
                    onDeleteFailed.accept(file)
                }
            }
            return flag
        } else
            return false
    }

}
