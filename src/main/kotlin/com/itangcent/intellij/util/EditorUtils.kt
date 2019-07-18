package com.itangcent.intellij.util

import com.intellij.openapi.editor.Editor

/**
 * @author tangcent
 */
object EditorUtils {
    fun caretMoveToOffset(editor: Editor, offset: Int) {
        val caretModel = editor.caretModel
        caretModel.currentCaret.moveToOffset(offset)
    }

    fun currentOffset(editor: Editor): Int {
        val caretModel = editor.caretModel
        return caretModel.currentCaret.offset
    }
}
