package com.itangcent.intellij.logger

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.itangcent.common.utils.Functions
import com.itangcent.intellij.PLUGIN_NAME
import com.itangcent.intellij.constant.EventKey.Companion.ON_COMPLETED
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.utils.ApplicationUtils
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A logger implementation that outputs messages to the IntelliJ IDEA console window.
 * This class creates and manages a tool window in the IDE's bottom panel for displaying log messages.
 *
 * @author tangcent
 */
@Singleton
open class IdeaConsoleLogger : Logger {

    @Inject
    protected lateinit var project: Project

    @Inject
    protected lateinit var actionContext: ActionContext

    @Inject(optional = true)
    @Named(PLUGIN_NAME)
    protected val pluginName: String = "intellij-plugin"

    // Queue to store log messages before flushing to the console
    private val logQueue = ConcurrentLinkedQueue<Pair<Level, String>>()

    // Flag to track if log processing is active
    private val processing = AtomicBoolean(false)

    /**
     * Lazily initialized console view that handles the actual display of log messages.
     * Creates a tool window in the IDE's bottom panel if it doesn't exist,
     * or updates the existing one if it does.
     */
    private val consoleView: ConsoleView by lazy {
        // Create a new console builder for the current project
        val console = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console

        // Run UI operations in the Swing EDT thread
        registerConsoleView(console)

        // Use exactlyOnce to ensure the END message is only printed once,
        // even if ON_COMPLETED is triggered multiple times
        actionContext.on(ON_COMPLETED, Functions.exactlyOnce<ActionContext> {
            printLog(Level.INFO, "------------------END----------------––")
        })
        console
    }

    protected open fun registerConsoleView(console: ConsoleView) {
        actionContext.runInSwingUI {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            var toolWindow = toolWindowManager.getToolWindow(pluginName)
            if (toolWindow == null) {
                // Register a new tool window if it doesn't exist
                toolWindow = toolWindowManager.registerConsole(id = pluginName, console = console)
            } else {
                toolWindow.setContent(console)
            }
            toolWindow.show()
        }
    }

    protected open fun ToolWindowManager.registerConsole(
        id: String,
        console: ConsoleView
    ): ToolWindow = registerToolWindow(
        RegisterToolWindowTask(
            id = id,
            anchor = ToolWindowAnchor.BOTTOM,
            canCloseContent = false,
            component = console.component
        )
    )

    protected fun ToolWindow.setContent(console: ConsoleView) {
        if (ApplicationUtils.isIdeaVersionLessThan2023) {
            // Remove existing content if any
            this.contentManager.removeAllContents(true)

            // Add new content
            val content = this.contentManager.factory.createContent(
                console.component,
                "",
                false
            )
            this.contentManager.addContent(content)
        } else {
            // Update existing tool window with the new console component
            this.contentManager.getContent(0)?.component = console.component
        }
    }

    /**
     * Processes and queues a log message.
     * Instead of immediately sending to console, adds to queue and triggers flush if needed.
     */
    override fun log(level: Level, msg: String) {
        // Add log to queue
        logQueue.add(level to msg)

        tryFlushLogs()
    }

    private fun tryFlushLogs() {
        if (processing.compareAndSet(false, true)) {
            flushLogs()
        }
    }

    /**
     * Flushes queued logs to the console in a single UI operation.
     * This reduces the number of runInSwingUI calls.
     */
    private fun flushLogs() {
        // Only make one UI call with the collected logs
        actionContext.runInSwingUI {
            generateSequence { logQueue.poll() }
                .forEach { (level, log) ->
                    printLog(level, log)
                }
            processing.set(false)
            if (logQueue.isNotEmpty()) {
                // If there are still logs in the queue, try to continue processing
                tryFlushLogs()
            }
        }
    }

    private fun printLog(level: Level, log: String) {
        val contentType = when (level) {
            Level.DEBUG -> ConsoleViewContentType.LOG_DEBUG_OUTPUT
            Level.INFO -> ConsoleViewContentType.LOG_INFO_OUTPUT
            Level.WARN -> ConsoleViewContentType.LOG_WARNING_OUTPUT
            Level.ERROR -> ConsoleViewContentType.LOG_ERROR_OUTPUT
            else -> ConsoleViewContentType.NORMAL_OUTPUT
        }
        consoleView.print("[${level.name}]\t$log\n", contentType)
    }
}

/**
 * @deprecated Use IdeaConsoleLogger instead
 */
@Deprecated("Use IdeaConsoleLogger instead", ReplaceWith("IdeaConsoleLogger"))
typealias ConsoleRunnerLogger = IdeaConsoleLogger