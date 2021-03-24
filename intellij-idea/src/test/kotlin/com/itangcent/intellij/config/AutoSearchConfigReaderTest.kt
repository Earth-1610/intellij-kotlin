package com.itangcent.intellij.config

import com.google.inject.Inject
import com.intellij.openapi.module.Module
import com.itangcent.common.utils.ResourceUtils
import com.itangcent.common.utils.forceMkdirParent
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.psi.ContextSwitchListener
import com.itangcent.mock.AdvancedContextTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals

/**
 * Test case of [AutoSearchConfigReader]
 */
class AutoSearchConfigReaderTest : AdvancedContextTest() {

    @Inject
    protected lateinit var configReader: ConfigReader

    override fun beforeBind() {
        super.afterBind()
        //load configs from resource to tempDir as files in module
        for (file in configFiles) {
            File("$tempDir${s}config${s}$file")
                .also { it.forceMkdirParent() }
                .also { it.createNewFile() }
                .writeBytes(ResourceUtils.findResource("config${s}$file")!!.readBytes())

            File("$tempDir${s}config${s}a${s}$file")
                .also { it.forceMkdirParent() }
                .also { it.createNewFile() }
                .writeBytes(ResourceUtils.findResource("config${s}a${s}$file")!!.readBytes())
        }
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(ConfigReader::class) { it.with(TestPathSearchConfigReader::class) }

        //mock mockContextSwitchListener
        val mockModule = Mockito.mock(Module::class.java)
        Mockito.`when`(mockModule.moduleFilePath).thenReturn("$tempDir${s}config${s}a")
        val mockContextSwitchListener = Mockito.mock(ContextSwitchListener::class.java)
        Mockito.`when`(mockContextSwitchListener.getModule()).thenReturn(mockModule)
        builder.bind(ContextSwitchListener::class.java) { it.toInstance(mockContextSwitchListener) }
    }

    @Test
    fun testConfig() {
        assertEquals("cc", configReader.first("config.c"))
        assertEquals("ca", configReader.first("config.a"))
        assertEquals("cy", configReader.first("config.y"))
        assertEquals("cac", configReader.first("config.a.c"))
        assertEquals("caa", configReader.first("config.a.a"))
        assertEquals("cay", configReader.first("config.a.y"))
    }
}

val configFiles = listOf(".test.config", ".test.yaml", ".test.yml")

class TestPathSearchConfigReader : AutoSearchConfigReader() {

    @PostConstruct
    fun init() {
        loadConfigInfo()
    }

    override fun configFileNames(): List<String> {
        return configFiles
    }

}