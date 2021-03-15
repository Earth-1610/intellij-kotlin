package com.itangcent.test

import com.itangcent.common.files.*
import com.itangcent.common.utils.FileUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Test case of [com.itangcent.common.files]
 */
class FilesTest {

    private var tempDir: Path? = null
    private val separator = File.separator

    /**
     * build dir and files:
     * temp--A--a--1.txt
     *      ├B--b--2.java
     *      └C--c--3.kt
     *         └d--3.txt
     *            └4.txt
     */
    @BeforeEach
    fun before(@TempDir tempDir: Path) {
        this.tempDir = tempDir
        tempDir.sub("A/a/1.txt").init()
        tempDir.sub("B/b/2.java").init()
        tempDir.sub("C/c/3.kt").init()
        tempDir.sub("C/d/3.txt").init()
        tempDir.sub("C/d/4.txt").init()
    }

    private fun File.init() {
        FileUtils.forceMkdirParent(this)
        FileUtils.write(this, "hello world")
    }

    private fun Path.sub(path: String): File {
        return File("$this/$path".r())
    }

    /**
     * redirect to real path
     */
    private fun String.r(): String {
        return this.replace("/", separator)
    }

    @Test
    fun testTravel() {
        val folders = arrayListOf<String>()
        val files = arrayListOf<String>()
        val traveler = DefaultFileTraveler(tempDir.toString())
            .exceptDir("$tempDir/A".r())

        traveler.copy()
            .onFolder(FileHandle.from { folders.add(it.path()) })
            .onFile(FileHandle.collectFiles(files) { it.path() })
            .filter(FileFilter.filterFile { it.name().endsWith(".txt") })
            .onCompleted(object : FileCompleted {
                override fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long) {
                    assertEquals(2, fileCnt)
                }
            })
            .travel()

        assertEquals(6, folders.size)
        assertEquals(2, files.size)

        traveler.copy()
            .filter(FileFilter.filterFile { it.name().endsWith(".kt") })
            .filter(FileFilter.filterDirectory { it.name() != "B" })
            .onCompleted(object : FileCompleted {
                override fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long) {
                    assertEquals(1, fileCnt)
                }
            }.andThen(object : FileCompleted {
                override fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long) {
                    assertEquals(6, folderCnt)
                }
            }))
            .travel()

        //assert that folders no change
        assertEquals(6, folders.size)

        //assert that files no change
        assertEquals(2, files.size)

        val ktFiles = arrayListOf<FileWrap>()
        traveler.copy()
            .filter(FileFilter.filterFile { it.name().endsWith(".kt") })
            .onFile(FileHandle.collectFiles(ktFiles))
            .travel()

        assertEquals(1, ktFiles.size)
        ModelBaseTest.equal(FileWrap(tempDir.toString(), tempDir!!.sub("C/c/3.kt")), ktFiles[0])

        //travel unexisted directory
        DefaultFileTraveler(tempDir.toString() + "/Z")
            .onFile(FileHandle.from { Assertions.fail() })
            .travel()

        traveler.copy()
            .onFolder(FileHandle.from { folders.add(it.path()) })
            .onFile(FileHandle.collectFiles(files) { it.path() })
            .filter(FileFilter.filterFile { it.name().endsWith(".txt") })
            .onCompleted(object : FileCompleted {
                override fun onCompleted(fileCnt: Int, folderCnt: Int, time: Long) {
                    assertEquals(2, fileCnt)
                }
            })
            .travel()
    }

    @Test
    fun testFileWrap() {
        val fileWrap = FileWrap(tempDir.toString(), tempDir!!.sub("A/a/1.txt"))
        assertEquals(tempDir.toString(), fileWrap.root())
        assertEquals("1.txt", fileWrap.name())
        assertEquals("$tempDir/A/a/1.txt".r(), fileWrap.path())
        assertEquals("hello world", fileWrap.content())
        assertEquals("$tempDir/A/a".r(), fileWrap.parent())
        fileWrap.write("new")
        assertEquals("new", fileWrap.content())
        fileWrap.rename("2.txt")
        assertEquals("new", FileUtils.read(tempDir!!.sub("A/a/2.txt")))
        assertEquals("", FileWrap(tempDir.toString(), tempDir!!.sub("A/a/999.txt")).content())
        assertDoesNotThrow { FileWrap(tempDir.toString(), tempDir!!.sub("A/a/999.txt")).write("some") }
        assertFalse(fileWrap.equals(""))

    }
}