package com.itangcent.intellij.logger

import com.google.inject.Singleton
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.itangcent.intellij.constant.EventKey.Companion.ON_COMPLETED

/**
 * Logger that creates unique tool windows 
 * with IDs in format {pluginName}(n). Reuses completed windows.
 */
@Singleton
class MultiToolWindowConsoleLogger : IdeaConsoleLogger() {

    private var currentToolWindowId: String? = null

    /**
     * Creates a new tool window with unique ID or reuses a completed one.
     */
    override fun registerConsoleView(console: ConsoleView) {
        actionContext.runInSwingUI {
            val toolWindow: ToolWindow

            synchronized(MultiToolWindowConsoleLogger) {
                val toolWindowManager = ToolWindowManager.getInstance(project)

                // Get unique window ID
                val toolWindowId = toolWindowManager.generateNewWindowId()

                // Check if window exists and can be reused
                val existedToolWindow = toolWindowManager.getToolWindow(toolWindowId)

                toolWindow = if (existedToolWindow != null) {
                    existedToolWindow.setContent(console)
                    existedToolWindow
                } else {
                    // Create new window
                    toolWindowManager.registerConsole(id = toolWindowId, console = console)
                }

                removeFromAvailablePool(toolWindowId)
                currentToolWindowId = toolWindowId
            }

            toolWindow.show()

            // Mark window as available for reuse when completed
            actionContext.on(ON_COMPLETED) {
                synchronized(MultiToolWindowConsoleLogger) {
                    if (currentToolWindowId != null && !isWindowInPool(currentToolWindowId!!)) {
                        addToAvailablePool(currentToolWindowId!!)
                    }
                }
            }
        }
    }

    private fun ToolWindowManager.generateNewWindowId(): String {
        var toolWindowId = pluginName
        var existedToolWindow = getToolWindow(toolWindowId)
        if (existedToolWindow != null && !isWindowInPool(toolWindowId)) {
            var idx = 1
            while (existedToolWindow != null && !isWindowInPool(toolWindowId)) {
                toolWindowId = "$pluginName(${idx++})"
                existedToolWindow = getToolWindow(toolWindowId)
            }
        }
        return toolWindowId
    }

    private fun removeFromAvailablePool(windowId: String) {
        completedWindows.remove("${project.name}/$windowId")
    }

    private fun isWindowInPool(windowId: String): Boolean {
        return completedWindows.contains("${project.name}/$windowId")
    }

    private fun addToAvailablePool(windowId: String) {
        completedWindows.add("${project.name}/$windowId")
    }

    companion object {
        // Stores completed window IDs for reuse
        private val completedWindows = mutableListOf<String>()
    }
} 