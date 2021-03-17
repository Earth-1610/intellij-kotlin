package com.itangcent.common.utils

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.StringWriter
import java.util.stream.Stream
import kotlin.test.assertEquals

/**
 * Test case of [com.itangcent.common.utils.StreamKit]
 */
class StreamKitTest {

    private val array: Array<String> = arrayOf(
        "China", "Germany", "France", "Israel",
        "Philippines", "Azerbaijan", "Brazil", "Denmark", "Swaziland"
    )

    private val list = arrayListOf(*array)

    private val stream: Stream<String>
        get() = Stream.of(*array)

    @CsvSource(
        value = [
            ",|[|]|-1|...|[China,Germany,France,Israel,Philippines,Azerbaijan,Brazil,Denmark,Swaziland]",
            ",|[|]|2|...|[China,Germany,...]",
            ",|[|]|2|~|[China,Germany,~]",
            "''|''|''|-1|...|ChinaGermanyFranceIsraelPhilippinesAzerbaijanBrazilDenmarkSwaziland"
        ], delimiter = '|',
        nullValues = ["null"]
    )
    @ParameterizedTest
    fun testJoinToString(
        separator: CharSequence,
        prefix: CharSequence,
        postfix: CharSequence,
        limit: Int,
        truncated: CharSequence,
        ret: String
    ) {
        assertEquals(
            ret,
            stream.joinToString(
                separator, prefix, postfix,
                limit, truncated
            )
        )
    }

    @ValueSource(
        classes = [
            StringBuilder::class,
            StringBuffer::class,
            StringWriter::class
        ]
    )
    @ParameterizedTest
    fun testJoinTo(cls: Class<Appendable>) {
        val appendable = cls.newInstance()
        assertEquals(
            "China, Germany, France, Israel, Philippines, Azerbaijan, Brazil, Denmark, Swaziland",
            stream.joinTo(appendable).toString()
        )
    }

    @Test
    fun testAppendElement() {
        val sb = StringBuilder()
        sb.appendElement(1, null)
        assertEquals(sb.toString(), "1")
        sb.appendElement(2) { (it + 1).toString() }
        assertEquals(sb.toString(), "13")
        sb.appendElement("123") { "1" }
        assertEquals(sb.toString(), "131")
        sb.appendElement('4', null)
        assertEquals(sb.toString(), "1314")
    }

    @Test
    fun testFirstOrNull() {
        assertEquals("China", stream.firstOrNull())
        assertEquals("Germany", stream.firstOrNull { it.first() == 'G' })
    }

    @Test
    fun testStream() {
        assertEquals(9, array.stream().count())
        assertEquals(0, (null as Array<*>?).stream().count())
        assertEquals(0, emptyArray<Any?>().stream().count())
    }

    @Test
    fun testReduceSafely() {
        assertEquals("ChinaGermanyFranceIsraelPhilippinesAzerbaijanBrazilDenmarkSwaziland",
            stream.reduceSafely { a, b -> a + b })
        assertEquals(null, emptyArray<String>().stream().reduceSafely { a, b -> a + b })
    }

    @Test
    fun testFilterAs() {
        assertEquals(0, stream.filterAs<Int>().count())
        assertEquals(9, stream.filterAs<CharSequence>().count())
        assertEquals(0, stream.filterAs(Int::class).count())
        assertEquals(9, stream.filterAs(CharSequence::class).count())
        assertEquals(0, list.filterAs<Int>().count())
        assertEquals(9, list.filterAs<CharSequence>().count())
        assertEquals(0, list.filterAs(Int::class).count())
        assertEquals(9, list.filterAs(CharSequence::class).count())
    }

    @Test
    fun testMapToTypedArray() {
        assertArrayEquals(arrayOf('C', 'G', 'F', 'I', 'P', 'A', 'B', 'D', 'S'), array.mapToTypedArray { it.first() })
        assertArrayEquals(arrayOf('C', 'G', 'F', 'I', 'P', 'A', 'B', 'D', 'S'), list.mapToTypedArray { it.first() })
    }

    @Test
    fun testFilterNotNull() {
        assertEquals(9, stream.filterNotNull().count())
        assertEquals(2, Stream.of("a", null, "b").filterNotNull().count())
    }

    @Test
    fun testMapNotNull() {
        assertEquals("C, G, F, I, P, A, B, D, S", stream.mapNotNull { it.first() }.joinToString())
        assertEquals("aa, bb", Stream.of("a", null, "b").mapNotNull { "$it$it" }.joinToString())
    }
}