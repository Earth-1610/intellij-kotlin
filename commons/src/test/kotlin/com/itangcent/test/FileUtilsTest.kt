package com.itangcent.test

import com.itangcent.common.utils.FileUtils
import com.itangcent.common.utils.forceDelete
import com.itangcent.common.utils.forceMkdir
import com.itangcent.common.utils.forceMkdirParent
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue


class FileUtilsTest {

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
    fun testSearch() {
        //search A/a/1.txt,C/d/3.txt,C/d/4.txt
        assertEquals(3, FileUtils.search("$tempDir", "*.txt").size)

        //search A/a/1.txt,C/d/3.txt,C/c/3.kt,C/d/4.txt
        assertEquals(4, FileUtils.search("$tempDir", "*.*t").size)

        //search C/c/3.kt,C/d/3.txt
        assertEquals(2, FileUtils.search("$tempDir", "3.*").size)
    }

    @Test
    fun testWriteAndRead() {
        testWriteAndRead(Charsets.UTF_8)
        testWriteAndRead(Charsets.UTF_16)
        testWriteAndRead(Charsets.UTF_16BE)
        testWriteAndRead(Charsets.UTF_16LE)
        testWriteAndRead(Charsets.US_ASCII)
        testWriteAndRead(Charsets.ISO_8859_1)
        testWriteAndRead(Charsets.UTF_32)
        testWriteAndRead(Charsets.UTF_32LE)
        testWriteAndRead(Charsets.UTF_32BE)
    }

    private fun testWriteAndRead(charset: Charset) {
        FileUtils.write(tempDir!!.sub("A/a/1-1.txt"), "hello world", charset)
        assertEquals("hello world", FileUtils.read(tempDir!!.sub("A/a/1-1.txt"), charset))
    }

    @Test
    fun testWriteAndReadBytes() {
        FileUtils.write(tempDir!!.sub("A/a/1-2.txt"), byteArrayOf(1, 2, 3, 4))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), FileUtils.readBytes(tempDir!!.sub("A/a/1-2.txt"))!!)
        assertDoesNotThrow { FileUtils.readBytes(tempDir!!.sub("A/a/999.txt")) }
    }

    @Test
    fun testForceMkdir() {
        val subDir = tempDir!!.sub("a/b/c")
        FileUtils.forceMkdir(subDir)
        assertTrue(subDir.exists())
        val d = tempDir!!.sub("a/b/d")
        d.forceMkdir()
        assertTrue(d.exists())
        assertThrows<IOException> {
            tempDir!!.sub("A/a/1.txt").forceMkdir()
        }
    }

    @Test
    fun testForceMkdirParent() {
        val subDir = tempDir!!.sub("a/b/c")
        FileUtils.forceMkdirParent(subDir)
        assertTrue(tempDir!!.sub("a/b").exists())
        val d = tempDir!!.sub("a/b/c/d")
        d.forceMkdirParent()
        assertTrue(tempDir!!.sub("a/b/c").exists())
    }

    @Test
    fun testForceDelete() {
        val ONE = tempDir!!.sub("A/a/1.txt")
        FileUtils.forceDelete(ONE)
        assertFalse(ONE.exists())
        val d = tempDir!!.sub("C")
        d.forceDelete()
        assertFalse(tempDir!!.sub("C").exists())
        assertFalse(tempDir!!.sub("C/d/4.txt").exists())
    }

    @Test
    fun testRenameFile() {
        FileUtils.renameFile(tempDir!!.sub("A/a/1.txt"), "1-1.txt")
        assertEquals("hello world", FileUtils.read(tempDir!!.sub("A/a/1-1.txt")))

        FileUtils.renameFile("$tempDir/B/b".r(), "2.java", "2-1.txt")
        assertEquals("hello world", FileUtils.read(tempDir!!.sub("B/b/2-1.txt")))

        FileUtils.renameFile(tempDir!!.sub("C/d/3.txt"), "4.txt")
        assertFalse(tempDir!!.sub("C/d/3.txt").exists())

        tempDir!!.sub("B/b/2.java").let {
            assertSame(it, FileUtils.renameFile(it, "2.java"))
        }
    }

    @Test
    fun testMove() {
        FileUtils.move(tempDir!!.sub("A/a/1.txt"), "$tempDir/B/b".r())
        assertEquals("hello world", FileUtils.read(tempDir!!.sub("B/b/1.txt")))
    }

    @Test
    fun testRemove() {
        FileUtils.remove(tempDir!!.sub("A/a/1.txt"))
        assertFalse(tempDir!!.sub("A/a/1.txt").exists())
    }

    @Test
    fun testIsEmptyDir() {
        assertFalse(FileUtils.isEmptyDir(tempDir!!.sub("A/a/1.txt")))
        assertFalse(FileUtils.isEmptyDir(tempDir!!.sub("A/a")))
        FileUtils.remove(tempDir!!.sub("A/a/1.txt"))
        assertTrue(FileUtils.isEmptyDir(tempDir!!.sub("A/a")))
    }

    @Test
    fun testIsSymlink() {
        assertThrows<NullPointerException> { FileUtils.isSymlink(null) }
        assertFalse(FileUtils.isSymlink(tempDir!!.sub("A/a/1.txt")))
    }

    @Test
    fun testCleanDirectory() {
        FileUtils.cleanDirectory(tempDir!!.sub("C"))
        assertTrue(tempDir!!.sub("C").exists())
        assertTrue(FileUtils.isEmptyDir(tempDir!!.sub("C")))
    }

    @Test
    fun testCleanEmptyDir() {
        assertFalse(FileUtils.cleanEmptyDir(tempDir!!.sub("C/c/3.kt")))
        tempDir!!.sub("C/c/3.kt").forceDelete()

        FileUtils.cleanEmptyDir(tempDir!!.sub("C"))
        assertTrue(tempDir!!.sub("C").exists())
        assertTrue(tempDir!!.sub("C/d").exists())
        assertFalse(tempDir!!.sub("C/c").exists())

        FileUtils.cleanEmptyDir(tempDir!!.sub("B")) {
            Assertions.fail()
        }
    }
}