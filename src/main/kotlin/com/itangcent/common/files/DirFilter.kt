package com.itangcent.common.files

/**
 * Created by TomNg on 2017/2/12.
 */
@FunctionalInterface
interface DirFilter {
    fun accept(file: FileWrap): Boolean

    companion object {

        val defaultHandle: DirFilter = object : DirFilter {
            override fun accept(file: FileWrap): Boolean {
                return true
            }
        }

        fun from(filter: (file: FileWrap) -> Boolean): DirFilter {
            return object : DirFilter {
                override fun accept(file: FileWrap): Boolean = filter(file)
            }
        }

        fun andThen(firstHandle: DirFilter, nextHandle: DirFilter): DirFilter {
            return object : DirFilter {
                override fun accept(file: FileWrap): Boolean = firstHandle.accept(file) && nextHandle.accept(file)
            }
        }
    }
}

public fun DirFilter.andThen(nextHandle: DirFilter): DirFilter = DirFilter.from { file ->
    this.accept(file) && nextHandle.accept(file)
}

public fun DirFilter.andThen(nextHandle: (file: FileWrap) -> Boolean): DirFilter = DirFilter.from { file ->
    this.accept(file) && nextHandle(file)
}
