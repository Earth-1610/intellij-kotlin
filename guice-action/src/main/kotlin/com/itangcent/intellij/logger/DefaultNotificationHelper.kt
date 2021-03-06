package com.itangcent.intellij.logger

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId

@Singleton
class DefaultNotificationHelper : NotificationHelper {

    @Inject(optional = true)
    @Named("plugin.name")
    protected val pluginName: String = "default_notify"

    @Inject(optional = true)
    protected val notificationDisplayType: NotificationDisplayType = NotificationDisplayType.BALLOON

    @Inject(optional = true)
    protected val project: Project? = null

    @Volatile
    private var notificationGroup: NotificationGroup? = null

    @Synchronized
    fun getNotificationGroup(): NotificationGroup {
        if (notificationGroup == null) {
            val notificationId = "${pluginName}_Notification"
            notificationGroup = when (notificationDisplayType) {
                NotificationDisplayType.BALLOON ->
                    NotificationGroup.balloonGroup(notificationId)
                NotificationDisplayType.TOOL_WINDOW ->
                    NotificationGroup.toolWindowGroup(notificationId, ToolWindowId.RUN, true)
                else ->
                    NotificationGroup.logOnlyGroup(notificationId)
            }
        }
        return notificationGroup!!
    }

    override fun notify(notify: (NotificationGroup) -> Notification) {
        val notification = notify(getNotificationGroup())
        Notifications.Bus.notify(notification, project)
    }

}