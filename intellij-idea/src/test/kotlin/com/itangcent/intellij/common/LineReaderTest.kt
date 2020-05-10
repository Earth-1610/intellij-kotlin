package com.itangcent.intellij.common

import com.itangcent.intellij.config.LineReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LineReaderTest {

    @Test
    fun testOneLine() {
        LineReader("key=value") {
            assertEquals("key=value", it)
        }.lines()
        LineReader("key=value\\\n&1\\\n&2") {
            assertEquals("key=value&1&2", it)
        }.lines()
        LineReader("key=js:```\nvar x = 1;\nreturn x+1;\n```") {
            assertEquals("key=js:\nvar x = 1;\nreturn x+1;", it)
        }.lines()
    }

}