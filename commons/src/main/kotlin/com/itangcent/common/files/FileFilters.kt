package com.itangcent.common.files

typealias FileFilter = (FileWrap) -> Boolean

/**
 * Created by tangcent on 2017/2/12.
 */
object FileFilters {

    val defaultHandle: FileFilter = { true }

    fun filterFile(filter: (file: FileWrap) -> Boolean): FileFilter {
        return { it.file.isDirectory || filter(it) }
    }

    fun filterDirectory(filter: (file: FileWrap) -> Boolean): FileFilter {
        return { it.file.isFile || filter(it) }
    }
}

fun FileFilter.andThen(nextHandle: FileFilter): FileFilter = { file ->
    this(file) && nextHandle(file)
}