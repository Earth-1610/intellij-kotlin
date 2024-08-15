package com.itangcent.intellij.config

class LineReader(private val content: String) {

    /**
     * Processes each line of the content using the provided lineHandle function.
     */
    fun lines(lineHandle: (String) -> Unit) {
        LineReaderProcessor(content, lineHandle).process()
    }

    /**
     * Returns a list of all processed lines.
     */
    fun lines(): MutableList<String> {
        val result = mutableListOf<String>()
        lines { result.add(it) }
        return result
    }
}

private class LineReaderProcessor(
    private val content: String,
    private val lineHandle: (String) -> Unit
) {
    private var oneLine = StringBuilder()
    private var status: Status = Default

    /**
     * Processes the content by splitting it into lines and handling each line
     * according to the current status.
     */
    fun process() {
        content.lines()
            .map { it.trimEnd() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                status.handle(line, this)
            }
        if (oneLine.isNotEmpty()) {
            lineHandle(oneLine.toString())
        }
    }

    /**
     * Finalizes the current line and resets the status.
     */
    private fun nextLine() {
        lineHandle(oneLine.toString())
        oneLine.clear()
        status = Default
    }

    /**
     * Interface representing the state for handling lines.
     */
    interface Status {
        fun LineReaderProcessor.handle(line: String)
    }

    fun Status.handle(line: String, lineReaderProcessor: LineReaderProcessor) {
        lineReaderProcessor.handle(line)
    }

    /**
     * Default state for handling lines.
     */
    object Default : Status {
        override fun LineReaderProcessor.handle(line: String) {
            when {
                line.endsWith("\\") -> {
                    oneLine.append(line.removeSuffix("\\"))
                    status = Wrap
                }

                line.endsWith("```") -> {
                    val backticks = line.takeLastWhile { it == '`' }
                    oneLine.append(line.removeSuffix(backticks))
                    status = Block(backticks)
                }

                else -> lineHandle(line.trimStart())
            }
        }
    }

    /**
     * State for handling lines that are wrapped with a backslash.
     */
    object Wrap : Status {
        override fun LineReaderProcessor.handle(line: String) {
            if (line.endsWith("\\")) {
                oneLine.append(line.removeSuffix("\\"))
            } else {
                oneLine.append(line)
                nextLine()
            }
        }
    }

    /**
     * State for handling lines within a code block.
     */
    class Block(private val backticks: String) : Status {
        override fun LineReaderProcessor.handle(line: String) {
            if (line.endsWith(backticks)) {
                val content = line.removeSuffix(backticks)
                if (content.isNotEmpty()) {
                    oneLine.appendLine().append(content)
                }
                nextLine()
            } else {
                oneLine.appendLine().append(line)
            }
        }
    }
}
