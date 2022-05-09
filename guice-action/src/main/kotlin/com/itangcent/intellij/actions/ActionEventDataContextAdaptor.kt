package com.itangcent.intellij.actions

import com.google.inject.Inject
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.itangcent.intellij.context.ActionContext

class ActionEventDataContextAdaptor : DataContext {

    @Inject(optional = true)
    private var actionEvent: AnActionEvent? = null

    constructor()

    constructor(actionEvent: AnActionEvent?) {
        this.actionEvent = actionEvent
    }

    override fun <T : Any?> getData(key: DataKey<T>): T? {
        val context = ActionContext.getContext()
        return if (context == null) {
            actionEvent!!.getData(key)
        } else {
            context.callInReadUI {
                actionEvent!!.getData(key)
            }
        }
    }

    override fun getData(dataId: String): Any? {
        val context = ActionContext.getContext()
        return if (context == null) {
            actionEvent!!.getData(DataKey.create(dataId))
        } else {
            context.callInReadUI {
                actionEvent!!.getData(DataKey.create(dataId))
            }
        }
    }
}