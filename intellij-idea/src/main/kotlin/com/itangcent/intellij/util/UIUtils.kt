package com.itangcent.intellij.util

import com.itangcent.common.function.ResultHolder
import com.itangcent.common.utils.ClassHelper
import java.awt.Dialog
import java.awt.EventQueue
import kotlin.reflect.KClass

object UIUtils {
    fun show(dialog: Dialog) {
        EventQueue.invokeLater {
            dialog.pack()
            dialog.isVisible = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Dialog> create(dialogCls: KClass<T>): T {
        val resultHolder = ResultHolder<T>()
        EventQueue.invokeLater {
            val dialog: Dialog = ClassHelper.newInstance(dialogCls) as Dialog
            dialog.pack()
            dialog.isVisible = true
            resultHolder.setResultVal(dialog as T)
        }
        return resultHolder.getResultVal()!!
    }
}