package com.itangcent.intellij.util

import com.itangcent.common.concurrent.ValueHolder
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
        val resultHolder = ValueHolder<T>()
        EventQueue.invokeLater {
            resultHolder.compute {
                val dialog: Dialog = ClassHelper.newInstance(dialogCls) as Dialog
                dialog.pack()
                dialog.isVisible = true
                return@compute dialog as T
            }
        }
        return resultHolder.value()!!
    }
}