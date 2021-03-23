package com.itangcent.intellij.adaptor

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.itangcent.intellij.adaptor.ModuleAdaptor.file
import com.itangcent.intellij.adaptor.ModuleAdaptor.filePath
import com.itangcent.mock.AdvancedContextTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Test case of [ModuleAdaptor]
 */
internal class ModuleAdaptorTest : AdvancedContextTest() {

    private lateinit var module: Module
    private lateinit var moduleRoot: VirtualFile

    override fun setUp() {
        super.setUp()
        //mock Module
        val mockModule = Mockito.mock(Module::class.java)
        Mockito.`when`(mockModule.moduleFilePath).thenReturn("$tempDir${s}app")
        val mockVirtualFile = Mockito.mock(VirtualFile::class.java)
        Mockito.`when`(mockModule.moduleFile).thenReturn(mockVirtualFile)
        this.module = mockModule
        this.moduleRoot = mockVirtualFile
    }

    @Test
    fun testFilePath() {
        assertEquals("$tempDir${s}app", this.module.filePath())
    }

    @Test
    fun testFile() {
        assertSame(moduleRoot, this.module.file())
    }
}