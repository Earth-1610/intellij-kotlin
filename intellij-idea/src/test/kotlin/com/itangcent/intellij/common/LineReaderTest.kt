package com.itangcent.intellij.common

import com.itangcent.intellij.config.LineReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test case for [LineReader]
 */
class LineReaderTest {

    @Test
    fun testOneLine() {
        LineReader("key=value").lines {
            assertEquals("key=value", it)
        }
        LineReader("key=value\\\n&1\\\n&2").lines {
            assertEquals("key=value&1&2", it)
        }
        LineReader("key=js:```\nvar x = 1;\nreturn x+1;\n```").lines {
            assertEquals("key=js:\nvar x = 1;\nreturn x+1;", it)
        }
        LineReader("key=js:```\nvar x = 1;\nreturn x+1;\n```").lines {
            assertEquals("key=js:\nvar x = 1;\nreturn x+1;", it)
        }
        LineReader(
            """key=``````
```xml
<!-- https://mvnrepository.com/artifact/com.itangcent/intellij-idea -->
<dependency>
    <groupId>com.itangcent</groupId>
    <artifactId>intellij-idea</artifactId>
    <version>1.6.6</version>
    <scope>runtime</scope>
</dependency>
```
``````
"""
        ).lines {
            assertEquals(
                """key=
```xml
<!-- https://mvnrepository.com/artifact/com.itangcent/intellij-idea -->
<dependency>
    <groupId>com.itangcent</groupId>
    <artifactId>intellij-idea</artifactId>
    <version>1.6.6</version>
    <scope>runtime</scope>
</dependency>
```""", it
            )
        }
    }

    @Test
    fun testMultiLine() {
        LineReader(
            """key1=value1
key2=value2
key3=value3
"""
        ).lines().let { lines ->
            assertEquals("key1=value1", lines[0])
            assertEquals("key2=value2", lines[1])
            assertEquals("key3=value3", lines[2])
        }

        LineReader(
            """key1=```
value1
value2
```"""
        ).lines().let { lines ->
            assertEquals("key1=\nvalue1\nvalue2", lines[0])
        }

        LineReader(
            """key1=```
value1
value2```
"""
        ).lines().let { lines ->
            assertEquals("key1=\nvalue1\nvalue2", lines[0])
        }

        LineReader(
            """key1=```
value1
"""
        ).lines().let { lines ->
            assertEquals("key1=\nvalue1", lines[0])
        }
    }
}