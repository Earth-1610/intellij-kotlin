package com.itangcent.common.files

/**
 * Created by TomNg on 2017/2/12.
 */
@FunctionalInterface
interface FileHandle {
    fun handle(file: FileWrap)

    companion object {

        val defaultHandle: FileHandle = object : FileHandle {
            override fun handle(file: FileWrap) {}
        }

        fun from(filter: (file: FileWrap) -> Unit): FileHandle {
            return object : FileHandle {
                override fun handle(file: FileWrap) {
                    filter(file)
                }
            }
        }

        fun collectFiles(files: MutableList<FileWrap>): FileHandle {
            return object : FileHandle {
                override fun handle(file: FileWrap) {
                    files.add(file)
                }

            }
        }

        fun <T> collectFiles(files: MutableList<T>, transform: (FileWrap) -> T): FileHandle {
            return object : FileHandle {
                override fun handle(file: FileWrap) {
                    files.add(transform(file))
                }

            }
        }
    }
}

public fun FileHandle.andThen(nextHandle: FileHandle): FileHandle = FileHandle.from { file ->
    this.handle(file)
    nextHandle.handle(file)
}
