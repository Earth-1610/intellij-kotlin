package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.mock.AdvancedContextTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test case of [AbstractConfigReader]
 */
internal class AbstractConfigReaderTest : AdvancedContextTest() {

    @Inject
    private lateinit var configReader: ConfigReader

    private val mutableConfigReader: MutableConfigReader by lazy {
        configReader as MutableConfigReader
    }

    override fun customConfig(): String {
        return "a=1\n" +
                "b=21\n" +
                "b=22\n" +
                "c=3\${a}\n" +
                "c=3\${a+1}\n" +
                "c=3\${a*3}\n" +
                "sum=\${a+b+c}\n" +
                "product=\${a*b*c}\n" +
                "concat=\${a}\${b}\${c}\n" +
                "ternary=\${a>1?0:1}\${b>a?0:1}\${c>b?0:1}\n" +
                "object={a:\${a},b:\${b},c:\${c}}\n" +
                "array=[\${a},\${b},\${c}]\n" +
                "ultimate=\${object}-\${object.a}-\${array}-\${array[0]}\n" +
                "literal=\${'a+b+c'}\n" +
                "literal=\${\"a*b*c\"}\n" +
                "literal=\${'a+b*c\"}\n" + //will be resolved as ""
                "candidate=abc\n" +
                "candidate=a\n" +
                "candidate=abcd\n" +
                "candidate=ab\n" +
                "###set resolveMulti = error\n" +
                "error=\${candidate}\n" +
                "###set resolveMulti = first\n" +
                "first=\${candidate}\n" +
                "###set resolveMulti = last\n" +
                "last=\${candidate}\n" +
                "###set resolveMulti = longest\n" +
                "longest=\${candidate}\n" +
                "###set resolveMulti = shortest\n" +
                "shortest=\${candidate}\n" +
                "###set resolveProperty = false\n" +
                "notResolveProperty=\${candidate}\n"+
                "###set resolveProperty = true\n"
    }

    @Test
    fun testConfigReader() {
        //test first & read
        assertEquals("1", configReader.first("a"))
        assertEquals(listOf("1"), configReader.read("a")?.toList())
        assertEquals("21", configReader.first("b"))
        assertEquals(listOf("21", "22"), configReader.read("b")?.toList())
        assertEquals("31", configReader.first("c"))
        assertEquals(listOf("31", "32", "33"), configReader.read("c")?.toList())
        assertEquals("53", configReader.first("sum"))
        assertEquals("651", configReader.first("product"))
        assertEquals("12131", configReader.first("concat"))
        assertEquals("{a:1,b:21,c:31}", configReader.first("object"))
        assertEquals("[1,21,31]", configReader.first("array"))
        assertEquals("{a:1,b:21,c:31}-1-[1,21,31]-1", configReader.first("ultimate"))
        assertEquals("a+b+c", configReader.first("literal"))
        assertEquals(listOf("a+b+c", "a*b*c", ""), configReader.read("literal")?.toList())

        assertEquals("", configReader.first("error"))
        assertEquals("abc", configReader.first("first"))
        assertEquals("ab", configReader.first("last"))
        assertEquals("abcd", configReader.first("longest"))
        assertEquals("a", configReader.first("shortest"))
        assertEquals("\${candidate}", configReader.first("notResolveProperty"))

        //test foreach
        run {
            var ret = ""
            val action: (String, String) -> Unit = { key, value ->
                ret += "$key=$value\n"
            }

            configReader.foreach(action)
            assertEquals(
                "a=1\n" +
                        "b=21\n" +
                        "b=22\n" +
                        "c=31\n" +
                        "c=32\n" +
                        "c=33\n" +
                        "sum=53\n" +
                        "product=651\n" +
                        "concat=12131\n" +
                        "ternary=100\n" +
                        "object={a:1,b:21,c:31}\n" +
                        "array=[1,21,31]\n" +
                        "ultimate={a:1,b:21,c:31}-1-[1,21,31]-1\n" +
                        "literal=a+b+c\n" +
                        "literal=a*b*c\n" +
                        "literal=\n" +
                        "candidate=abc\n" +
                        "candidate=a\n" +
                        "candidate=abcd\n" +
                        "candidate=ab\n" +
                        "error=\n" +
                        "first=abc\n" +
                        "last=ab\n" +
                        "longest=abcd\n" +
                        "shortest=a\n" +
                        "notResolveProperty=\${candidate}\n", ret
            )

            ret = ""
            configReader.foreach({ it.startsWith("c") }, action)
            assertEquals(
                "c=31\n" +
                        "c=32\n" +
                        "c=33\n" +
                        "concat=12131\n" +
                        "candidate=abc\n" +
                        "candidate=a\n" +
                        "candidate=abcd\n" +
                        "candidate=ab\n", ret
            )
        }

        //test resolveProperty
        assertEquals("1-210~31x", configReader.resolveProperty("\${a > 1?0:1}-\${b * 10}~\${c + \"x\"}"))
    }

    @Test
    fun testMutableConfigReader() {
        assertEquals("1", configReader.first("a"))
        mutableConfigReader.reset()
        assertNull(configReader.first("a"))

        mutableConfigReader.put("x", "x1")
        assertEquals("x1", configReader.first("x"))
        assertEquals(listOf("x1"), configReader.read("x")?.toList())
        mutableConfigReader.remove("x")
        assertNull(configReader.first("x"))

        mutableConfigReader.put("y", "y1", "y2")
        assertEquals("y1", configReader.first("y"))
        assertEquals(listOf("y1", "y2"), configReader.read("y")?.toList())
        mutableConfigReader.remove("y", "y1")
        assertEquals("y2", configReader.first("y"))
        assertEquals(listOf("y2"), configReader.read("y")?.toList())

        mutableConfigReader.put("z", "z1", "z2", "z3")
        assertEquals("z1", configReader.first("z"))
        assertEquals(listOf("z1", "z2", "z3"), configReader.read("z")?.toList())
        mutableConfigReader.remove("z")
        assertNull(configReader.first("z"))

        mutableConfigReader.loadConfigInfoContent(customConfig())

        assertEquals("1", configReader.first("a"))
        assertEquals("{a:1,b:21,c:31}-1-[1,21,31]-1", configReader.first("ultimate"))
    }
}