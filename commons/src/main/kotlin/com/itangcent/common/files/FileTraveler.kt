package com.itangcent.common.files

/**
 * Created by tangcent on 2017/2/12.
 */
interface FileTraveler {

    fun exceptDir(vararg dirs: String): FileTraveler

    fun onFile(fileHandle: FileHandle): FileTraveler

    fun onDirectory(fileHandle: FileHandle): FileTraveler

    fun filter(fileFilter: FileFilter): FileTraveler

    fun onCompleted(fileCompleted: FileCompleted): FileTraveler

    fun travel()
}

@Deprecated(message = "for compatibility only", replaceWith = ReplaceWith("FileTraveler.onDirectory(fileHandle)"))
fun FileTraveler.onFolder(fileHandle: FileHandle): FileTraveler {
    return this.onDirectory(fileHandle)
}

fun FileTraveler.filterFile(fileFilter: FileFilter): FileTraveler {
    return this.filter(FileFilters.filterFile(fileFilter))
}

fun FileTraveler.filterDirectory(fileFilter: FileFilter): FileTraveler {
    return this.filter(FileFilters.filterDirectory(fileFilter))
}