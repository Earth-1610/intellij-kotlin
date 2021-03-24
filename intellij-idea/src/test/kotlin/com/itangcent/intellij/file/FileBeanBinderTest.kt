package com.itangcent.intellij.file

import com.itangcent.common.utils.GsonUtils
import com.itangcent.mock.AdvancedContextTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test case of [FileBeanBinder]
 */
internal class FileBeanBinderTest : AdvancedContextTest() {

    @ParameterizedTest
    @CsvSource(
        value = [
            "java.lang.String|\"a\"",
            "com.itangcent.intellij.file.FileBeanBinderTestPoint|{\"x\":1,\"y\":1}"
        ], delimiter = '|'
    )
    fun testFileBeanBinder(cls: Class<Any>, json: String) {
        val bean = GsonUtils.fromJson(json, cls)
        val fileBeanBinder = FileBeanBinder(File("$tempDir$s${cls.name}"), cls.kotlin)
        assertNull(fileBeanBinder.tryRead())
        fileBeanBinder.save(bean)
        assertEquals(bean, fileBeanBinder.read())
    }
}

data class FileBeanBinderTestPoint(
    var x: Int,
    var y: Int
)