package com.itangcent.intellij.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.itangcent.common.exception.ProcessCanceledException
import com.itangcent.common.logger.Log
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.context.ActionContextBuilder
import com.itangcent.intellij.logger.NotificationHelper
import org.apache.commons.lang3.exception.ExceptionUtils
import javax.swing.Icon

abstract class KotlinAnAction : AnAction {

    /**
     * The title of the action.
     */
    var title: String? = null
        protected set
    
    constructor() : super()
    constructor(icon: Icon?) : super(icon)
    constructor(text: String?) : super(text) {
        this.title = text
    }
    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon) {
        this.title = text
    }

    protected open fun onBuildActionContext(
        event: AnActionEvent,
        builder: ActionContextBuilder
    ) {
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return

        val actionContextBuilder = ActionContext.builder()
        actionContextBuilder.bindInstance(AnAction::class, this)
        actionContextBuilder.bindInstance(Project::class, project)
        actionContextBuilder.bindInstance(AnActionEvent::class, anActionEvent)
        actionContextBuilder.bind(DataContext::class) { it.toInstance(anActionEvent.dataContext) }
        onBuildActionContext(anActionEvent, actionContextBuilder)
        val actionContext = actionContextBuilder.build()
        actionContext.init(this)

        LOG.info("start action:" + this::class.qualifiedName)

        actionContext.runAsync {
            try {
                actionPerformed(actionContext, project, anActionEvent)
            } catch (ex: Exception) {
                LOG.info("Error:${ex.message}trace:${ExceptionUtils.getStackTrace(ex)}")
                NotificationHelper.instance().notify {
                    it.createNotification(
                        when (ex) {
                            is ProcessCanceledException -> ex.stopMsg ?: "Unknown"
                            else -> "Error at:${ex.message}trace:${ExceptionUtils.getStackTrace(ex)}"
                        },
                        NotificationType.ERROR
                    )
                }
            }
        }
        actionContext.waitCompleteAsync()
    }

    protected abstract fun actionPerformed(
        actionContext: ActionContext,
        project: Project?,
        anActionEvent: AnActionEvent
    )

    companion object : Log()
}

