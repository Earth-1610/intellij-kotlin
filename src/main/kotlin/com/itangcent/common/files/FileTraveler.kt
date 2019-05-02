package com.itangcent.common.files

/**
 * Created by TomNg on 2017/2/12.
 */
interface FileTraveler {
    fun exceptDir(vararg dirs: String): FileTraveler

    fun onFile(fileHandle: FileHandle): FileTraveler

    fun onFolder(fileHandle: FileHandle): FileTraveler

    fun filter(fileFilter: FileFilter): FileTraveler

    fun onCompleted(fileCompleted: FileCompleted): FileTraveler

    fun travel()
}
