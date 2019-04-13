package com.itangcent.intellij.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.apache.commons.lang.StringUtils
import java.util.*

object DocumentUtils {
    private val PMD_TAB_SIZE = 8

    fun calculateRealOffset(document: Document, line: Int, pmdColumn: Int): Int {
        val maxLine = document.lineCount
        if (maxLine < line) {
            return -1
        }
        val lineOffset = document.getLineStartOffset(line - 1)
        return lineOffset + calculateRealColumn(document, line, pmdColumn)
    }

    fun calculateRealColumn(document: Document, line: Int, pmdColumn: Int): Int {
        var realColumn = pmdColumn - 1
        val minusSize = PMD_TAB_SIZE - 1
        val docLine = line - 1
        val lineStartOffset = document.getLineStartOffset(docLine)
        val lineEndOffset = document.getLineEndOffset(docLine)
        val text = document.getText(TextRange(lineStartOffset, lineEndOffset))

        text.forEachIndexed { i, c ->
            if (c == '\t') {
                realColumn -= minusSize
            }
            if (i >= realColumn) {
                return@forEachIndexed
            }
        }

        return realColumn
    }

    fun getLineText(document: Document, line: Int): String {
        return document.getText(TextRange.create(document.getLineStartOffset(line), document.getLineEndOffset(line)))
    }

    fun getInsertIndex(document: Document): Int {
        val lineCount = document.lineCount
        for (line in lineCount - 1 downTo -1 + 1) {
            val lineText = DocumentUtils.getLineText(document, line)
            if (StringUtils.isBlank(lineText)) {
                continue
            }
            if (lineText.trim { it <= ' ' }.endsWith("}")) {
                return document.getLineEndOffset(line) - 1
            }
        }
        return document.textLength - 1;

    }

    private val rootStartSet = HashSet<String>()

    init {
        rootStartSet.add("class")
        rootStartSet.add("interface")
        rootStartSet.add("enum")
        rootStartSet.add("@interface")
    }

    //region check isRootStart--------------------------------------------------
    fun isRootStart(lineText: String): Boolean {
        val trimLineText = lineText.trim { it <= ' ' }
        if (trimLineText.startsWith("public")) {
            return true
        }
        for (rs in rootStartSet) {
            if (trimLineText.startsWith(rs)) {
                return true
            }
        }
        return false
    }
    //endregion check isRootStart--------------------------------------------------
}