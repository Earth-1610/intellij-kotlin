package com.itangcent.common.files

/**
 * Created by TomNg on 2017/2/12.
 */
interface FileFilter {
    fun accept(file: FileWrap): Boolean

    companion object {

        val defaultHandle: FileFilter = object : FileFilter {
            override fun accept(file: FileWrap): Boolean {
                return true
            }
        }

        fun from(filter: (file: FileWrap) -> Boolean): FileFilter {
            return object : FileFilter {
                override fun accept(file: FileWrap): Boolean = filter(file)
            }
        }

        fun filterFile(filter: (file: FileWrap) -> Boolean): FileFilter {
            return object : FileFilter {
                override fun accept(file: FileWrap): Boolean = file.file.isDirectory || filter(file)
            }
        }

        fun filterDirectory(filter: (file: FileWrap) -> Boolean): FileFilter {
            return object : FileFilter {
                override fun accept(file: FileWrap): Boolean = file.file.isFile || filter(file)
            }
        }

    }
}

public fun FileFilter.andThen(nextHandle: FileFilter): FileFilter = FileFilter.from { file ->
    this.accept(file) && nextHandle.accept(file)
}

public fun FileFilter.andThen(nextHandle: (file: FileWrap) -> Boolean): FileFilter = FileFilter.from { file ->
    this.accept(file) && nextHandle(file)
}

