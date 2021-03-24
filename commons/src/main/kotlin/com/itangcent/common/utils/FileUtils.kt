package com.itangcent.common.utils

import com.itangcent.common.files.DefaultFileTraveler
import com.itangcent.common.files.FileHandle
import com.itangcent.common.files.FileTraveler
import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.ILoggerProvider
import com.itangcent.common.spi.SpiUtils
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern

object FileUtils {

    fun search(localPath: String, filePattern: String): List<File> {
        val tinyLocalPath = localPath.removeSuffix(File.separator)
        val fileTraveler: FileTraveler = DefaultFileTraveler(tinyLocalPath)

        if (StringUtils.isNotEmpty(filePattern)) {
            var fp = resolveFilePattern(filePattern, tinyLocalPath)
            fp = formatPattern(fp)
            val pattern = Pattern.compile("^$fp$")

            fileTraveler.filter(com.itangcent.common.files.FileFilter.filterFile {
                pattern.matcher(it.name()).matches()
            })
        }
        val files = ArrayList<File>()
        fileTraveler.onFile(FileHandle.collectFiles(files) { it.file })
        fileTraveler.travel()
        return files
    }

    private fun resolveFilePattern(filePattern: String, tinyLocalPath: String): String {
        var filePattern1 = filePattern
        if (StringUtils.startsWith(filePattern1, ".${File.separator}")) {
            filePattern1 = "$tinyLocalPath${filePattern1.substring(1)}"
        } else if (StringUtils.startsWith(filePattern1, "~${File.separator}")) {
            val userHome = System.getProperty("user.home")
            filePattern1 = "$userHome${filePattern1.substring(1)}"
        }
        return filePattern1
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

    fun read(file: File): String? {
        return read(file, Charsets.UTF_8)
    }

    fun read(file: File, charSet: Charset): String? {
        return try {
            file.inputStream().use { it.readString(charSet) }
        } catch (e: Exception) {
            null
        }
    }

    fun readBytes(file: File): ByteArray? {
        return try {
            file.inputStream().use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    fun write(file: File, content: String) {
        write(file, content, Charsets.UTF_8)
    }

    fun write(file: File, content: String, charSet: Charset) {
        write(file, content.toByteArray(charSet))
    }

    fun write(file: File, content: ByteArray) {
        val out = FileOutputStream(file)
        out.use {
            out.write(content)
        }
    }

    fun renameFile(path: String, oldName: String, newName: String) {
        if (oldName != newName) {
            //Renaming is necessary if the new file name is different from the previous file name
            val oldFile = File("$path${File.separator}$oldName")
            if (!oldFile.exists()) {
                return //The rename file does not exist
            }
            val newFile = File("$path${File.separator}$newName")

            if (newFile.exists()) {
                if (!newFile.delete()) {
                    throw IllegalStateException("failed to rename [$oldName] to [$newName]!")
                }
            }

            oldFile.renameTo(newFile)
        }
    }

    fun renameFile(file: File, newName: String): File {
        if (file.name != newName) {
            //Renaming is necessary if the new file name is different from the previous file name
            val newPath = file.parent + File.separator + newName
            doRenameFile(file, newPath)
            return File(newPath)
        }
        return file
    }

    fun move(file: File, newPath: String) {
        if (file.parent != newPath) {
            doRenameFile(file, "$newPath${File.separator}${file.name}")
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
        return if (file.isDirectory) {
            val files = file.list()
            files == null || files.isEmpty()
        } else {
            false
        }
    }

    fun cleanEmptyDir(path: String) {
        val file = File(path)
        if (!file.exists()) {
            return
        }
        val errorFiles = Stack<File>()
        cleanEmptyDir(file) { errorFiles.push(it) }
        while (!errorFiles.empty()) {
            errorFiles.pop().delete()
        }
    }

    fun cleanEmptyDir(file: File): Boolean {
        return cleanEmptyDir(file, null)
    }

    fun cleanEmptyDir(file: File, onDeleteFailed: ((File) -> Unit)?): Boolean {
        var flag = true
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (subFile in files) {
                    if (subFile.isFile || !cleanEmptyDir(subFile)) {
                        flag = false
                    }
                }
            }
            if (flag) {
                if (!file.delete() && onDeleteFailed != null) {
                    onDeleteFailed(file)
                }
            }
            return flag
        } else
            return false

    }
    //-----------------------------------------------------------------------
    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     *
     *
     * The difference between File.delete() and this method are:
     *
     *  * A directory to be deleted does not have to be empty.
     *  * You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)
     *
     *
     * @param file file or directory to delete, must not be `null`
     * @throws NullPointerException  if the directory is `null`
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */

    fun forceDelete(file: File) {
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            val filePresent = file.exists()
            if (!file.delete()) {
                if (!filePresent) {
                    return
                }
                LOG?.error("Unable to delete file: $file")
            }
        }
    }
    //-----------------------------------------------------------------------
    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws IllegalArgumentException if `directory` does not exist or is not a directory
     */

    fun deleteDirectory(directory: File) {
        if (!directory.exists()) {
            return
        }
        if (!isSymlink(directory)) {
            cleanDirectory(directory)
        }
        if (!directory.delete()) {
            LOG?.error("Unable to delete directory $directory.")
        }
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     *
     *
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     *
     *
     * When using jdk1.7, this method delegates to `boolean java.nio.file.Files.isSymbolicLink(Path path)`
     *
     * **Note:** the current implementation always returns `false` if running on
     * jkd1.6 and the system is detected as Windows using [FilenameUtils.isSystemWindows]
     *
     *
     * For code that runs on Java 1.7 or later, use the following method instead:
     * <br></br>
     * `boolean java.nio.file.Files.isSymbolicLink(Path path)`
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     * @since 2.0
     */

    fun isSymlink(file: File?): Boolean {
        if (file == null) {
            LOG?.error("File must not be null")
            return false
        }
        return Files.isSymbolicLink(file.toPath())
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException              in case cleaning is unsuccessful
     * @throws IllegalArgumentException if `directory` does not exist or is not a directory
     */
    fun cleanDirectory(directory: File) {
        val files: Array<File> = verifiedListFiles(directory)
        for (file in files) {
            try {
                forceDelete(file)
            } catch (ioe: IOException) {
                LOG?.error("File cleanDirectory:$file")
            }
        }
    }

    /**
     * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
     * @param directory The directory to list
     * @return The files in the directory, never null.
     * @throws IOException if an I/O error occurs
     */
    private fun verifiedListFiles(directory: File): Array<File> {
        if (!directory.exists()) {
            val message = "$directory does not exist"
            LOG?.error(message)
        }
        if (!directory.isDirectory) {
            val message = "$directory is not a directory"
            LOG?.error(message)
        }
        val listFiles = directory.listFiles()
        if (listFiles == null) {// null if security restricted
            LOG?.error("Failed to list contents of $directory")
            return emptyArray()
        }
        return listFiles
    }

    /**
     * Makes a directory, including any necessary but nonexistent parent
     * directories. If a file already exists with specified name but it is
     * not a directory then an IOException is thrown.
     * If the directory cannot be created (or does not already exist)
     * then an IOException is thrown.
     *
     * @param directory directory to create, must not be `null`
     * @throws NullPointerException if the directory is `null`
     * @throws IOException          if the directory cannot be created or the file already exists but is not a directory
     */
    fun forceMkdir(directory: File) {
        if (directory.exists()) {
            if (!directory.isDirectory) {
                val message = ("File "
                        + directory
                        + " exists and is "
                        + "not a directory. Unable to create directory.")
                throw IOException(message)
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory) {
                    LOG?.error("Unable to create directory $directory")
                }
            }
        }
    }

    /**
     * Makes any necessary but nonexistent parent directories for a given File. If the parent directory cannot be
     * created then an IOException is thrown.
     *
     * @param file file with parent to create, must not be `null`
     * @throws NullPointerException if the file is `null`
     * @throws IOException          if the parent directory cannot be created
     * @since 2.5
     */
    fun forceMkdirParent(file: File) {
        val parent = file.parentFile ?: return
        forceMkdir(parent)
    }
}

fun File.forceMkdirParent() {
    return FileUtils.forceMkdirParent(this)
}

fun File.forceMkdir() {
    return FileUtils.forceMkdir(this)
}

fun File.forceDelete() {
    return FileUtils.forceDelete(this)
}


//background idea log
private val LOG: ILogger? = SpiUtils.loadService(ILoggerProvider::class)?.getLogger(FileUtils::class)