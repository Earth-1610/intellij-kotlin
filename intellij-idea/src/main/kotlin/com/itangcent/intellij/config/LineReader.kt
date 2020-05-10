package com.itangcent.intellij.config

import com.itangcent.common.utils.append
import com.itangcent.common.utils.appendln

class LineReader(private val content: String, private val lineHandle: (String) -> Unit) {

    private var oneLine = ""

    private var status = common

    fun lines() {
        loop@ for (line in content.lines()) {
            val trimLine = line.trim()
            when (status) {
                common -> {
                    when {
                        trimLine.endsWith("\\") -> {
                            oneLine = oneLine.append(trimLine.removeSuffix("\\"))!!
                            status = wrap
                        }
                        trimLine.endsWith("```") -> {
                            oneLine = oneLine.append(trimLine.removeSuffix("```"))!!
                            status = block
                        }
                        else -> lineHandle(trimLine)
                    }
                }
                wrap -> {
                    if (trimLine.endsWith("\\")) {
                        oneLine += trimLine.removeSuffix("\\")
                        continue@loop
                    } else {
                        oneLine += trimLine
                        next()
                        continue@loop
                    }
                }
                block -> {
                    if (trimLine.endsWith("```")) {
                        oneLine = oneLine.appendln(trimLine.removeSuffix("```"))!!
                        next()
                        continue@loop
                    } else {
                        oneLine = oneLine.appendln(trimLine)!!
                        continue@loop
                    }
                }
            }
        }
    }

    /**
     * line end.
     * start read next line.
     */
    private fun next() {
        lineHandle(oneLine)
        oneLine = ""
        status = common
    }

    companion object {
        private const val common = 1
        private const val wrap = 2
        private const val block = 3
    }
}