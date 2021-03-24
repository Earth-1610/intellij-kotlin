package com.itangcent.intellij.file

import com.itangcent.common.utils.GsonUtils
import com.itangcent.mock.AdvancedContextTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito
import org.mockito.internal.verification.Times
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test case of [CachedBeanBinder]
 */
internal class CachedBeanBinderTest : AdvancedContextTest() {

    @ParameterizedTest
    @CsvSource(
        value = [
            "java.lang.String|\"a\"",
            "com.itangcent.intellij.file.CachedBeanBinderTestPoint|{\"x\":1,\"y\":1}"
        ], delimiter = '|'
    )
    fun testFileBeanBinder(cls: Class<Any>, json: String) {
        val bean = GsonUtils.fromJson(json, cls)
        val originFileBeanBinder = FileBeanBinder(File("$tempDir$s${cls.name}"), cls.kotlin)
        val spyBeanBinder = Mockito.spy(originFileBeanBinder)
        val fileBeanBinder = spyBeanBinder.lazy()

        assertNull(fileBeanBinder.tryRead())
        fileBeanBinder.save(bean)
        assertEquals(bean, fileBeanBinder.read())
        assertEquals(bean, fileBeanBinder.read())
        assertEquals(bean, fileBeanBinder.tryRead())

        //verify delegate method calls times as expected
        Mockito.verify(spyBeanBinder, Times(0)).read()
        Mockito.verify(spyBeanBinder, Times(1)).tryRead()

    }
}

data class CachedBeanBinderTestPoint(
    var x: Int,
    var y: Int
)