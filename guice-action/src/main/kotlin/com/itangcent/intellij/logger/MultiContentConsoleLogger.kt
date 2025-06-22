package com.itangcent.intellij.logger

import com.google.inject.Singleton
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.itangcent.common.utils.Functions
import com.itangcent.intellij.actions.KotlinAnAction
import com.itangcent.intellij.constant.EventKey.Companion.ON_COMPLETED
import com.itangcent.intellij.context.ActionContext

/**
 * Console logger that manages multiple console tabs within a single tool window.
 * Creates new content tabs for each console session and allows reuse of completed tabs.
 */
@Singleton
class MultiContentConsoleLogger : IdeaConsoleLogger() {

    private var currentContentId: String? = null

    /**
     * Registers console view by creating or reusing a tab in the plugin's tool window.
     */
    override fun registerConsoleView(console: ConsoleView) {
        actionContext.runInSwingUI {
            val toolWindow: ToolWindow

            synchronized(MultiContentConsoleLogger) {
                val toolWindowManager = ToolWindowManager.getInstance(project)

                // Get or create tool window with plugin name
                toolWindow = toolWindowManager.getToolWindow(pluginName)
                    ?: toolWindowManager.registerConsole(id = pluginName, console = console)

                // Generate content ID for this console session
                val contentId = toolWindow.getNextAvailableContentId()

                // Reuse completed content or create new tab
                val completedContent = toolWindow.findCompletedContent(contentId)

                if (completedContent != null) {
                    // Update existing tab
                    completedContent.component = console.component
                    completedContent.displayName = contentId
                    toolWindow.contentManager.setSelectedContent(completedContent)
                } else {
                    // Create new tab
                    val content = toolWindow.contentManager.factory.createContent(
                        console.component,
                        contentId,
                        false
                    )
                    toolWindow.contentManager.addContent(content)
                    toolWindow.contentManager.setSelectedContent(content)
                }
                addToActivePool(contentId)
                currentContentId = contentId
            }

            toolWindow.show()

            // Mark content as inactive when session completes
            actionContext.on(ON_COMPLETED, Functions.exactlyOnce<ActionContext> {
                synchronized(MultiContentConsoleLogger) {
                    if (currentContentId != null && isContentActive(currentContentId!!)) {
                        removeFromActivePool(currentContentId!!)
                    }
                }
            })
        }
    }

    /**
     * Add a content manager listener to auto-hide tool window when all contents are removed
     */
    private fun ToolWindow.addContentListener() {
        this.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                if (this@addContentListener.contentManager.contentCount == 0 && this@addContentListener.isVisible) {
                    this@addContentListener.hide()
                }
            }
        })
    }

    /**
     * Override the extension function to customize tool window creation
     */
    override fun ToolWindowManager.registerConsole(
        id: String,
        console: ConsoleView
    ): ToolWindow = registerToolWindow(
        RegisterToolWindowTask(
            id = id,
            anchor = ToolWindowAnchor.BOTTOM,
            canCloseContent = true
        )
    ).apply { addContentListener() }

    private fun ToolWindow.getNextAvailableContentId(): String {
        // Find unique ID for new content
        val existingContents = contentManager.contents
            .map { it.displayName }
            .filterNotNull()
            .toSet()

        var rawTitle = (actionContext.tryInstance(AnAction::class) as? KotlinAnAction)?.title ?: pluginName
        var toolWindowId = rawTitle
        if (existingContents.contains(toolWindowId) && isContentActive(toolWindowId)) {
            var idx = 1
            while (existingContents.contains(toolWindowId) && isContentActive(toolWindowId)) {
                toolWindowId = "$rawTitle(${idx++})"
            }
        }
        return toolWindowId
    }


    private fun ToolWindow.findCompletedContent(contentId: String): Content? {
        return contentManager.contents
            .firstOrNull { it.displayName == contentId && !isContentActive(it.displayName!!) }
    }

    private fun removeFromActivePool(contentId: String) {
        activeContents.remove("${project.name}/$contentId")
    }

    private fun isContentActive(contentId: String): Boolean {
        return activeContents.contains("${project.name}/$contentId")
    }

    private fun addToActivePool(contentId: String) {
        activeContents.add("${project.name}/$contentId")
    }

    companion object {
        // Track active content tabs across projects
        private val activeContents = mutableListOf<String>()

        // Track which tool windows already have content listeners
        private val toolContentListenerAdded = mutableSetOf<ToolWindow>()
    }
} 