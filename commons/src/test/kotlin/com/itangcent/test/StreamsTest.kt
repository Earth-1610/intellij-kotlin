package com.itangcent.test

import com.itangcent.common.utils.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test case of [Streams]
 */
class StreamsTest {


    private val array: Array<String> = arrayOf(
        "China", "Germany", "France", "Israel",
        "Philippines", "Azerbaijan", "Brazil", "Denmark", "Swaziland"
    )

    private val list = listOf(*array)

    private val stream: Stream<String>
        get() = Stream.of(*array)

    @Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
    @Test
    fun testToTypedArray() {
        //empty Integer[]
        Assertions.assertArrayEquals(arrayOf(), Stream.empty<Int>().toTypedArray())
        //Integer[]{1,2}
        Assertions.assertArrayEquals(arrayOf(1, 2), Stream.of(1, 2).toTypedArray())

        //empty Byte[]
        Assertions.assertArrayEquals(arrayOf(), Stream.empty<Byte>().toTypedArray() as Array<Byte>)
        //Byte[]{1,1}
        Assertions.assertArrayEquals(
            arrayOf<Byte>(0b1, 0b0),
            Stream.of<Byte>(0b1, 0b0).toTypedArray() as Array<Byte>
        )

        //empty String[]
        Assertions.assertArrayEquals(arrayOf(), Stream.empty<String>().toTypedArray())
        //String[]{1,2}
        Assertions.assertArrayEquals(arrayOf("1", "2"), Stream.of("1", "2").toTypedArray())

    }

    @ParameterizedTest
    @CsvSource(
        value = ["null,China", "0,China", "2,France"],
        nullValues = ["null"]
    )
    fun testSkip(i: Int?, str: String) {
        assertEquals(str, stream.skip(i).firstOrNull())
    }

    @Test
    fun testLongest() {
        assertEquals("Philippines", stream.longest())
        assertEquals("Philippines", array.longest())
        assertEquals("Philippines", list.longest())
    }

    @Test
    fun testShortest() {
        assertEquals("China", stream.shortest())
        assertEquals("China", stream.toTypedArray().shortest())
        assertEquals("China", list.shortest())
    }

    @Test
    fun testHead() {
        //stream
        assertEquals("Philippines", stream.head { it.length })
        assertEquals("Swaziland", stream.head { it.first() })
        assertEquals("Germany", stream.head { it.last() })
        assertEquals(null, (null as Stream<*>?).head { it.toString() })

        //array
        assertEquals("Philippines", array.head { it.length })
        assertEquals("Swaziland", array.head { it.first() })
        assertEquals("Germany", array.head { it.last() })
        assertEquals(null, (null as Array<*>?).head { it.toString() })
        assertEquals("x", (arrayOf("x")).head { it })

        //list
        assertEquals("Philippines", list.head { it.length })
        assertEquals("Swaziland", list.head { it.first() })
        assertEquals("Germany", list.head { it.last() })
        assertEquals(null, (null as List<*>?).head { it.toString() })
    }

    @Test
    fun testTail() {
        //stream
        assertEquals("China", stream.tail { it.length })
        assertEquals("Azerbaijan", stream.tail { it.first() })
        assertEquals("China", stream.tail { it.last() })
        assertEquals(null, (null as Stream<*>?).tail { it.toString() })

        //array
        assertEquals("China", array.tail { it.length })
        assertEquals("Azerbaijan", array.tail { it.first() })
        assertEquals("China", array.tail { it.last() })
        assertEquals(null, (null as Array<*>?).tail { it.toString() })
        assertEquals("x", (arrayOf("x")).tail { it })

        //list
        assertEquals("China", list.tail { it.length })
        assertEquals("Azerbaijan", list.tail { it.first() })
        assertEquals("China", list.tail { it.last() })
        assertEquals(null, (null as List<*>?).tail { it.toString() })
    }

}