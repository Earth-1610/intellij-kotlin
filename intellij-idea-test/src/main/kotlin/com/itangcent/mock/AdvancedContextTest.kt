package com.itangcent.mock

import com.itangcent.common.spi.Setup
import com.itangcent.common.utils.FileUtils
import com.itangcent.intellij.bindPluginName
import com.itangcent.intellij.config.BaseConfigReader
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.context.ActionContextBuilder
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.file.AbstractLocalFileRepository
import com.itangcent.intellij.file.LocalFileRepository
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.intellij.psi.DefaultPsiClassHelper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


/**
 * BaseContextTest with [tempDir]
 * Use junit5
 */
abstract class AdvancedContextTest : BaseContextTest() {

    @JvmField
    @TempDir
    var tempDir: Path? = null

    open fun customConfig(): String? {
        return null
    }

    override fun bind(builder: ActionContextBuilder) {
        super.bind(builder)
        builder.bindPluginName("intellij-kotlin")
        builder.bind(LocalFileRepository::class) {
            it.toInstance(TempFileRepository())
        }
        builder.bind(LocalFileRepository::class, "projectCacheRepository") {
            it.toInstance(TempFileRepository())
        }

        customConfig()?.takeIf { it.isNotBlank() }
            ?.let { config ->
                builder.bind(ConfigReader::class) {
                    it.toInstance(ConfigReaderAdaptor(config))
                }
            }

        builder.bind(PsiClassHelper::class) { it.with(DefaultPsiClassHelper::class).singleton() }
    }

    private inner class TempFileRepository : AbstractLocalFileRepository() {
        override fun basePath(): String {
            return tempDir.toString()
        }
    }

    private inner class ConfigReaderAdaptor(val config: String) : BaseConfigReader() {

        @PostConstruct
        fun init() {
            loadConfigInfoContent(config, "properties")
        }
    }

    protected val n = System.lineSeparator()
    protected val s = File.separator

    @AfterEach
    fun clearFile() {
        tempDir?.toFile()?.let { FileUtils.cleanDirectory(it) }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun load() {
            Setup.load()
        }
    }
}