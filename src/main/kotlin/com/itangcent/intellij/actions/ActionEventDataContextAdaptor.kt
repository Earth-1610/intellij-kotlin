package com.itangcent.intellij.actions

import com.google.inject.Inject
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey

class ActionEventDataContextAdaptor : DataContext {

    @Inject
    private val actionEvent: AnActionEvent? = null

    override fun <T : Any?> getData(key: DataKey<T>): T? {
        return actionEvent!!.getData(key)
    }

    override fun getData(dataId: String?): Any? {
        if (dataId == null) {
            return null
        }
        return actionEvent!!.getData(DataKey.create(dataId))
    }
}