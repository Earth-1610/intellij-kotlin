package com.itangcent.intellij.logger

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.itangcent.intellij.PLUGIN_NAME
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.utils.ApplicationUtils

/**
 * A logger implementation that outputs messages to the IntelliJ IDEA console window.
 * This class creates and manages a tool window in the IDE's bottom panel for displaying log messages.
 *
 * @author tangcent
 */
@Singleton
class IdeaConsoleLogger : AbstractLogger() {

    @Inject
    private lateinit var project: Project

    @Inject
    private lateinit var actionContext: ActionContext

    @Inject(optional = true)
    @Named(PLUGIN_NAME)
    private val pluginName: String = "intellij-plugin"

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
        actionContext.runInSwingUI {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            var toolWindow = toolWindowManager.getToolWindow(pluginName)
            if (toolWindow == null) {
                // Register a new tool window if it doesn't exist
                toolWindow = toolWindowManager.registerToolWindow(
                    RegisterToolWindowTask(
                        id = pluginName,
                        anchor = ToolWindowAnchor.BOTTOM,
                        canCloseContent = false,
                        component = console.component
                    )
                )
            } else {
                if (ApplicationUtils.isIdeaVersionLessThan2023) {
                    // Remove existing content if any
                    toolWindow.contentManager.removeAllContents(true)

                    // Add new content
                    val content = toolWindow.contentManager.factory.createContent(
                        console.component,
                        "",
                        false
                    )
                    toolWindow.contentManager.addContent(content)
                } else {
                    // Update existing tool window with the new console component
                    toolWindow.contentManager.getContent(0)?.component = console.component
                }
            }
            toolWindow.show()
        }

        console
    }

    /**
     * Processes and displays a log message in the console.
     *
     * @param logData The message to be logged. If null, the method returns without action.
     */
    override fun processLog(logData: String?) {
        if (logData == null) return

        actionContext.runInSwingUI {
            consoleView.print("$logData\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
    }
}

/**
 * @deprecated Use IdeaConsoleLogger instead
 */
@Deprecated("Use IdeaConsoleLogger instead", ReplaceWith("IdeaConsoleLogger"))
typealias ConsoleRunnerLogger = IdeaConsoleLogger