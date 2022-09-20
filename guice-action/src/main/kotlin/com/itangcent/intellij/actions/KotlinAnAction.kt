package com.itangcent.intellij.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.itangcent.common.exception.ProcessCanceledException
import com.itangcent.common.logger.Log
import com.itangcent.common.spi.Setup
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.logger.NotificationHelper
import org.apache.commons.lang3.exception.ExceptionUtils
import javax.swing.Icon

abstract class KotlinAnAction : AnAction {

    constructor() : super()
    constructor(icon: Icon?) : super(icon)
    constructor(text: String?) : super(text)
    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon)

    @Deprecated(replaceWith = ReplaceWith("LOG"), message = "use LOG instead")
    private val log = LOG

    protected open fun onBuildActionContext(
        event: AnActionEvent,
        builder: ActionContext.ActionContextBuilder
    ) {
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return

        val actionContextBuilder = ActionContext.builder()
        actionContextBuilder.bindInstance(Project::class, project)
        actionContextBuilder.bindInstance(AnActionEvent::class, anActionEvent)
        actionContextBuilder.bind(DataContext::class) { it.with(ActionEventDataContextAdaptor::class).singleton() }
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

    companion object : Log() {
        init {
            Setup.load(KotlinAnAction::class.java.classLoader)
        }
    }
}

