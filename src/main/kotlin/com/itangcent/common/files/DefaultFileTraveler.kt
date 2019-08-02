package com.itangcent.common.files

import java.io.File
import java.util.*

/**
 * Created by tangcent on 2017/2/12.
 */
class DefaultFileTraveler(private val root: String) : FileTraveler {

    private var onFile = FileHandle.defaultHandle
    private var onFolder = FileHandle.defaultHandle

    private var onCompleted = FileCompleted.defaultHandle
    private var fileFilter = FileFilter.defaultHandle
    private var dirFilter = FileFilter.defaultHandle

    override fun exceptDir(vararg dirs: String): FileTraveler {
        val exceptDir = HashSet(Arrays.asList(*dirs))
        dirFilter = dirFilter.andThen { file ->
            exceptDir.contains(file.file.path)
        }
        return this
    }

    override fun onFile(fileHandle: FileHandle): FileTraveler {
        this.onFile = this.onFile.andThen(fileHandle)
        return this
    }

    override fun onFolder(fileHandle: FileHandle): FileTraveler {
        this.onFolder = this.onFolder.andThen(fileHandle)
        return this
    }

    override fun filter(fileFilter: FileFilter): FileTraveler {
        this.fileFilter = this.fileFilter.andThen(fileFilter)
        return this
    }

    override fun onCompleted(fileCompleted: FileCompleted): FileTraveler {
        this.onCompleted = this.onCompleted.andThen(fileCompleted)
        return this
    }

    override fun travel() {
        RealTraveler().travel()
    }

    fun copy(): DefaultFileTraveler {
        val traveler = DefaultFileTraveler(this.root)
        traveler.dirFilter = this.dirFilter
        traveler.fileFilter = this.fileFilter
        traveler.onCompleted = this.onCompleted
        traveler.onFile = this.onFile
        traveler.onFolder = this.onFolder
        return traveler
    }

    private inner class RealTraveler {
        private var fileCnt = 0
        private var folderCnt = 0
        private val start = System.currentTimeMillis()
        private val files = LinkedList<File>()

        internal fun travel() {
            doTravel()
            onCompleted.onCompleted(fileCnt, folderCnt, System.currentTimeMillis() - start)
        }

        internal fun doTravel() {
            val rootFile = File(root)
            if (!rootFile.exists()) {
                return
            }
            files.add(rootFile)
            var file: File? = files.poll()
            while (file != null) {
                travel(FileWrap(root, file))
                file = files.poll()
            }
        }

        internal fun travel(file: FileWrap) {
            if (file.file.isDirectory) {
                if (dirFilter.accept(file)) {
                    ++folderCnt
                    onFolder.handle(file)
                    try {
                        Collections.addAll(files, *file.file.listFiles()!!)
                    } catch (e: Exception) {
                    }

                }
            } else if (fileFilter.accept(file)) {
                ++fileCnt
                onFile.handle(file)
            }
        }
    }
}
