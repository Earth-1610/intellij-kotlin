package com.itangcent.mock

import com.google.inject.Inject
import com.intellij.openapi.project.Project
import com.itangcent.intellij.config.ConfigReader
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.intellij.logger.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import org.mockito.Mockito

/**
 * Test case with [ActionContext]
 * Use junit5
 */
abstract class BaseContextTest {

    @Inject
    protected lateinit var actionContext: ActionContext

    @Inject
    protected lateinit var logger: Logger

    @BeforeEach
    fun buildContext() {
        beforeBind()
        val builder = ActionContext.builder()
        builder.bind(Logger::class) { it.with(PrintLogger::class) }
        builder.bind(Project::class) { it.toInstance(mockProject) }
        builder.bind(ConfigReader::class) { it.toInstance(mockConfigReader) }
        builder.bind(DevEnv::class) { it.toInstance(mockDevEnv) }
        bind(builder)
        val actionContext = builder.build()
        try {
            actionContext.init(this)
            afterBind(actionContext)
        } catch (e: Exception) {
            e.printStackTrace()
            fail("buildContext failed")
        }
        setUp()
    }

    protected open fun beforeBind() {
    }

    protected open fun afterBind(actionContext: ActionContext) {
    }

    protected open fun setUp() {

    }

    protected open fun bind(builder: ActionContext.ActionContextBuilder) {}

    @AfterEach
    fun tearDown() {
        actionContext.waitComplete()
        actionContext.stop()
        doTearDown()
    }

    protected open fun doTearDown() {
    }

    companion object {
        val mockProject = Mockito.mock(Project::class.java)
        val mockConfigReader = Mockito.mock(ConfigReader::class.java)
        val mockDevEnv = Mockito.mock(DevEnv::class.java)
    }
}