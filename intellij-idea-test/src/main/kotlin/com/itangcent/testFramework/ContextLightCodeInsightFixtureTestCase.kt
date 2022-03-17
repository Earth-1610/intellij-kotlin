package com.itangcent.testFramework

import com.google.inject.Inject
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.itangcent.common.spi.Setup
import com.itangcent.common.utils.ResourceUtils
import com.itangcent.common.utils.forceDelete
import com.itangcent.intellij.config.AbstractConfigReader
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.file.AbstractLocalFileRepository
import com.itangcent.intellij.file.LocalFileRepository
import com.itangcent.intellij.jvm.PsiClassHelper
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.psi.DefaultPsiClassHelper
import com.itangcent.mock.PrintLogger
import org.mockito.Mockito
import java.io.File
import java.net.URL
import java.security.AccessController
import java.security.PrivilegedAction
import kotlin.reflect.KClass


abstract class ContextLightCodeInsightFixtureTestCase : LightCodeInsightFixtureTestCase() {

    @Inject
    protected lateinit var actionContext: ActionContext

    @Inject
    protected lateinit var logger: Logger

    protected val tempDir: String = FileUtil.getTempDirectory()

    override fun getProjectDescriptor(): LightProjectDescriptor {
        //use java8
        return JAVA_8
    }

    open fun customConfig(): String? {
        return null
    }

    override fun setUp() {
        super.setUp()

        beforeBind()
        val builder = ActionContext.builder()
        builder.bind(Logger::class) { it.with(PrintLogger::class) }
        builder.bind(Project::class) { it.toInstance(this.project) }
        builder.bind(DevEnv::class) { it.toInstance(mockDevEnv) }
        builder.bind(ConfigReader::class) { it.toInstance(mockConfigReader) }

        builder.bindInstance("plugin.name", "intellij_kotlin")
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

        bind(builder)
        builder.build().init(this)
        afterBind()
    }

    protected open fun beforeBind() {
    }

    protected open fun afterBind() {
    }

    override fun tearDown() {
        try {
            actionContext.waitComplete()
            actionContext.stop(true)

            super.tearDown()
        } finally {
            File(tempDir).forceDelete()
        }
    }

    protected open fun bind(builder: ActionContext.ActionContextBuilder) {

    }

//    override fun shouldRunTest(): Boolean {
//        return !OS.WINDOWS.isCurrentOs
//    }

    /**
     * create psiClass but not configure to project
     * more lighter than [loadClass]
     */
    protected fun createClass(file: String, language: Language = JavaLanguage.INSTANCE): PsiClass? {
        return (createFile(file, language) as? PsiClassOwner)?.classes?.firstOrNull()
    }

    /**
     * create file but not configure to project
     * more lighter than [loadFile]
     */
    protected fun createFile(file: String, language: Language = JavaLanguage.INSTANCE): PsiFile? {
        val content = ResourceUtils.readResource(file)
        return createLightFile(file.substringAfterLast("/"), language, content)
    }

    /**
     * load psiClass from file to project
     */
    protected fun loadClass(file: String): PsiClass? {
        return (loadFile(file) as? PsiClassOwner)?.classes?.firstOrNull()
    }

    /**
     * load file to project
     */
    protected fun loadFile(file: String): PsiFile? {
        val content = ResourceUtils.readResource(file)
        myFixture.tempDirFixture.createFile(file, content)
        return myFixture.configureFromTempProjectFile(file)
    }

    /**
     * load source of the class to current project
     */
    protected fun loadSource(cls: KClass<*>): PsiClass? {
        return loadSource(cls.java)
    }

    protected fun loadSource(cls: Class<*>): PsiClass? {
        val readSourceFile = readSourceFile(cls)
        if (readSourceFile is PsiClassOwner) {
            return readSourceFile.classes.firstOrNull()
        }
        var path = readSourceFile.path
        var virtualFile = myFixture.tempDirFixture.getFile(path)
        if (virtualFile == null) {
            if (path.startsWith("/src")) {
                path = path.removePrefix("/src")
                virtualFile = myFixture.tempDirFixture.getFile(path)
            } else if (path.startsWith("\\src")) {
                path = path.removePrefix("\\src")
                virtualFile = myFixture.tempDirFixture.getFile(path)
            }
        }
        if (virtualFile?.exists() != true) {
            throw IllegalAccessException("failed load source $cls")
        }
        if (virtualFile is PsiClassOwner) {
            return virtualFile.classes.firstOrNull()
        }
        return (myFixture.configureFromTempProjectFile(path) as? PsiClassOwner)?.classes?.firstOrNull()
    }

    /**
     * @return target source file path
     */
    private fun readSourceFile(cls: Class<*>): VirtualFile {

        //try load from local
        var location = cls.protectionDomain?.codeSource?.location
        if (location == null) {
            location = AccessController.doPrivileged(PrivilegedAction {
                return@PrivilegedAction cls.protectionDomain?.codeSource?.location
            })
        }
        if (location != null) {
            return readSourceFile(location, cls)
        }

        val qualifiedName = cls.name
        if (!qualifiedName.contains('.')) {
            LOG.warn("Load [$qualifiedName] may be not meet expectation")
        }
        val path = qualifiedName.replace('.', '/') + ".java"

        //try load from resource/jdk
        ResourceUtils.readResource("jdk/${cls.simpleName}.fava")
            .takeIf { it.isNotBlank() }
            ?.let {
                return myFixture.tempDirFixture.createFile(path, it)
            }
        ResourceUtils.readResource("jdk/${cls.simpleName}.java")
            .takeIf { it.isNotBlank() }
            ?.let {
                return myFixture.tempDirFixture.createFile(path, it)
            }

        //try load resource from openjdk
        val resource = testCachedResourceResolver.resolve("$JDK/src/share/classes/${cls.name.replace('.', '/')}.java")
        resource.bytes?.let {
            return createVFile(path, it)
        }

        throw IllegalStateException("failed read source $cls")
    }

    /**
     *
     * @return target source file path
     */
    private fun readSourceFile(location: URL, cls: Class<*>): VirtualFile {
        val path = cls.name.replace('.', '/')
        try {
            URL("$location/$path.java").readBytes().also { content ->
                return createVFile("$path.java", content)
            }
        } catch (e: Exception) {
            //ignore
        }
        try {
            URL("$location/$path.class").readBytes().also { content ->
                return createVFile("$path.class", content)
            }
        } catch (e: Exception) {
            //ignore
        }
        throw IllegalAccessException("failed read source of $cls under $location")
    }

    private fun createVFile(path: String, content: ByteArray): VirtualFile {
        return myFixture.tempDirFixture.createFile(path).also {
            WriteAction.run<Exception> {
                it.setBinaryContent(content)
            }
        }
    }

    private inner class TempFileRepository : AbstractLocalFileRepository() {
        override fun basePath(): String {
            return tempDir
        }
    }

    private inner class ConfigReaderAdaptor(val config: String) : AbstractConfigReader() {

        @PostConstruct
        fun init() {
            loadConfigInfoContent(config, "properties")
        }

        override fun findConfigFiles(): List<String>? {
            return null
        }
    }

    companion object {
        init {
            print("load Setup")
            Setup.load()
        }

        private var testCachedResourceResolver = TestCachedResourceResolver()

        //mocks
        val mockConfigReader = Mockito.mock(ConfigReader::class.java)
        val mockDevEnv = Mockito.mock(DevEnv::class.java)

        //util
        val n = System.getProperty("line.separator")
        val s = File.separator
    }


    protected open val JDK
        get() = "https://raw.githubusercontent.com/openjdk/jdk/jdk8-b120/jdk"
}

//background idea log
private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ContextLightCodeInsightFixtureTestCase::class.java)
