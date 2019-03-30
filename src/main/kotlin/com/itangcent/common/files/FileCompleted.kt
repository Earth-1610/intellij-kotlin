package com.itangcent.common.files

/**
 * Created by TomNg on 2017/2/12.
 */
interface FileCompleted {
    /**
     * @param fileCnt -文件数
     * @param folderCnt  -文件夹数
     * @param time    -用时
     */
    fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long)


    fun andThen(nextHandle: FileCompleted): FileCompleted {
        val thisFileCompleted = this;
        return object : FileCompleted {
            override fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long) {
                thisFileCompleted.onCompleted(fileCnt, folderCnt, time)
                nextHandle.onCompleted(fileCnt, folderCnt, time)
            }
        }
    }

    companion object {

        val defaultHandle: FileCompleted = object : FileCompleted {
            override fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long) {}

            override fun andThen(nextHandle: FileCompleted): FileCompleted {
                return nextHandle
            }
        }
    }
}
