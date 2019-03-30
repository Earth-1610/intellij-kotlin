package com.itangcent.common.files

/**
 * Created by TomNg on 2017/2/12.
 */
interface FileTraveler {
    fun exceptDir(vararg dirs: String): FileTraveler

    /**
     * 文件回调
     *
     * @param fileHandle -文件回调
     */
    fun onFile(fileHandle: FileHandle): FileTraveler

    /**
     * 文件夹回调
     *
     * @param fileHandle -文件夹回调
     */
    fun onFolder(fileHandle: FileHandle): FileTraveler

    /**
     * 文件与文件夹过滤
     *
     * @param fileFilter -文件与文件夹过滤器
     */
    fun filter(fileFilter: FileFilter): FileTraveler

    /**
     * 文件遍历结束时回调
     *
     * @param fileCompleted -文件遍历结束时回调事件
     * @return
     */
    fun onCompleted(fileCompleted: FileCompleted): FileTraveler

    /**
     * 遍历文件
     */
    fun travel()
}
