package com.itangcent.intellij.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object ToolUtils {
    fun copy2Clipboard(str: String) {
        val selection = StringSelection(str)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }
}