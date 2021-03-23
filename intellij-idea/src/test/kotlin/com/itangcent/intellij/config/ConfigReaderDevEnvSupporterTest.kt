package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.mock.AdvancedContextTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Test case of [ConfigReaderDevEnvSupporter]
 */
internal class ConfigReaderDevEnvSupporterTest : AdvancedContextTest() {

    @Inject
    private lateinit var devEnv: DevEnv

    override fun customConfig(): String {
        return "dev=true"
    }

    override fun bind(builder: ActionContext.ActionContextBuilder) {
        super.bind(builder)
        builder.bind(DevEnv::class) { it.with(ConfigReaderDevEnvSupporter::class) }
    }

    @Test
    fun testIsDev() {
        assertTrue(devEnv.isDev())
    }

    @Test
    fun testDev() {
        var flag = false
        devEnv.dev {
            flag = true
        }
        assertTrue(flag)
    }
}