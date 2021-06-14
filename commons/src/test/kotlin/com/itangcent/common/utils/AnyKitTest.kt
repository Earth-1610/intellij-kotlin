package com.itangcent.common.utils

import com.itangcent.common.utils.*
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Test
import kotlin.test.*


/**
 * Test case of [AnyKit]
 */
class AnyKitTest {

    @Test
    fun testGetPropertyValue() {
        val point = AnyKitTestPoint(1, 2, 3)
        assertEquals(1, point.getPropertyValue("x"))
        assertEquals(2, point.getPropertyValue("y"))
        assertEquals(3, point.getPropertyValue("z"))
    }

    @Test
    fun testChangePropertyValue() {
        val point = AnyKitTestPoint(1, 2, 3)
        point.changePropertyValue("x", 11)
        point.changePropertyValue("y", 22)
        point.changePropertyValue("z", 33)
        assertEquals("{11, 22, 33}", point.toString())
    }


    @Test
    fun testInvokeMethod() {
        val point = AnyKitTestPoint(1, 2, 3)
        assertEquals(1, point.invokeMethod("updateX", 11))
        assertEquals(11, point.x)
        assertEquals(null, point.invokeMethod("updateY", 22))
        assertEquals(22, point.getY())
        assertEquals(null, point.invokeMethod("updateXYZ", 11, 22, 33))
        assertEquals("{11, 22, 33}", point.toString())
    }

    @Test
    fun testInvokeStaticMethod() {
        assertEquals(false, StringUtils::class.invokeStaticMethod("isEmpty", "s"))
    }

    @Test
    fun testChangePropertyValueByPropertyReference() {
        val point = AnyKitTestPoint(1, 2, 3)
        point.changePropertyValueByPropertyReference(point::x, 11)
        assertEquals("{11, 2, 3}", point.toString())
    }

    @Test
    fun testChangeValue() {
        val point = AnyKitTestPoint(1, 2, 3)
        point::x.changeValue(point, 11)
        assertEquals("{11, 2, 3}", point.toString())
    }

    @Test
    fun testPackageLevelGetPropertyValueByName() {
        assertEquals(8192, StringUtils::CR.packageLevelGetPropertyValueByName("PAD_LIMIT"))
        assertEquals(256, StringUtils::trimToEmpty.packageLevelGetPropertyValueByName("STRING_BUILDER_SIZE"))
    }

    @Test
    fun testPackageLevelInvokeMethodByName() {
        assertEquals("a", StringUtils::CR.packageLevelInvokeMethodByName("trim", " a "))
        assertEquals("ABC", StringUtils::trimToEmpty.packageLevelInvokeMethodByName("upperCase", "abc"))
    }

    @Test
    fun testCast() {
        assertEquals("a", "a".cast(String::class))
        assertNull("a".cast(Int::class))
        assertNull("a".cast(Long::class))
        assertNull(null.cast(String::class))
        assertNull(1.cast(String::class))
        assertEquals(1, 1.cast(Int::class))
        assertEquals(1, 1.cast(Number::class))
    }

    @Test
    fun testAnyIsNullOrEmpty() {
        assertTrue(null.anyIsNullOrEmpty())
        assertTrue("".anyIsNullOrEmpty())
        assertFalse(" ".anyIsNullOrEmpty())
        assertFalse("a".anyIsNullOrEmpty())
        assertTrue(emptyArray<String>().anyIsNullOrEmpty())
        assertFalse(arrayOf("a").anyIsNullOrEmpty())
        assertTrue(emptyList<String>().anyIsNullOrEmpty())
        assertFalse(listOf("a").anyIsNullOrEmpty())
        assertTrue(emptyMap<String, String>().anyIsNullOrEmpty())
        assertFalse(mapOf("a" to 1).anyIsNullOrEmpty())
        assertFalse(AnyKitTestPoint(1, 2, 3).anyIsNullOrEmpty())
    }

    @Test
    fun testAnyIsNullOrBlank() {
        assertTrue(null.anyIsNullOrBlank())
        assertTrue("".anyIsNullOrBlank())
        assertTrue(" ".anyIsNullOrBlank())
        assertFalse("a".anyIsNullOrBlank())
        assertFalse(emptyArray<String>().anyIsNullOrBlank())
        assertFalse(arrayOf("a").anyIsNullOrBlank())
        assertFalse(emptyList<String>().anyIsNullOrBlank())
        assertFalse(listOf("a").anyIsNullOrBlank())
        assertFalse(emptyMap<String, String>().anyIsNullOrBlank())
        assertFalse(mapOf("a" to 1).anyIsNullOrBlank())
        assertFalse(AnyKitTestPoint(1, 2, 3).anyIsNullOrBlank())
    }

    @Test
    fun testAsInt() {
        assertEquals(null, null.asInt())
        assertEquals(1, 1.asInt())
        assertEquals(1, true.asInt())
        assertEquals(0, false.asInt())
        assertEquals(1, 1L.asInt())
        assertEquals(123, "123".asInt())
        assertEquals(null, "abc".asInt())
        assertEquals(null, AnyKitTestPoint(1, 2, 3).asInt())
    }

    @Test
    fun testAsLong() {
        assertEquals(null, null.asLong())
        assertEquals(1L, 1.asLong())
        assertEquals(1L, true.asLong())
        assertEquals(0L, false.asLong())
        assertEquals(1L, 1L.asLong())
        assertEquals(123L, "123".asLong())
        assertEquals(null, "abc".asLong())
        assertEquals(null, AnyKitTestPoint(1, 2, 3).asLong())
    }

    @Test
    fun testAsFloat() {
        assertEquals(null, null.asFloat())
        assertEquals(1f, 1.0f.asFloat())
        assertEquals(1f, 1.asFloat())
        assertEquals(1f, true.asFloat())
        assertEquals(0f, false.asFloat())
        assertEquals(1f, 1L.asFloat())
        assertEquals(123f, "123".asFloat())
        assertEquals(123.456f, "123.456".asFloat())
        assertEquals(null, "abc".asFloat())
        assertEquals(null, AnyKitTestPoint(1, 2, 3).asFloat())
    }

    @Test
    fun testAsDouble() {
        assertEquals(null, null.asDouble())
        assertEquals(1.0, 1.0.asDouble())
        assertEquals(1.0, 1.0f.asDouble())
        assertEquals(1.0, 1.asDouble())
        assertEquals(1.0, true.asDouble())
        assertEquals(0.0, false.asDouble())
        assertEquals(1.0, 1L.asDouble())
        assertEquals(123.0, "123".asDouble())
        assertEquals(123.456, "123.456".asDouble())
        assertEquals(null, "abc".asDouble())
        assertEquals(null, AnyKitTestPoint(1, 2, 3).asDouble())
    }

    @Test
    fun testAsBool() {
        assertEquals(null, null.asBool())
        assertEquals(true, 1.0.asBool())
        assertEquals(true, 1.0f.asBool())
        assertEquals(true, 1.asBool())
        assertEquals(true, 1L.asBool())
        assertEquals(true, true.asBool())
        assertEquals(false, false.asBool())
        assertEquals(true, "true".asBool())
        assertEquals(true, "1".asBool())
        assertEquals(false, "123".asBool())
        assertEquals(false, "123.456".asBool())
        assertEquals(false, "abc".asBool())
        assertEquals(null, AnyKitTestPoint(1, 2, 3).asBool())
    }

    @Test
    fun testResolveCycle() {
        assertEquals(null, null.resolveCycle())
        assertSame(1, 1.resolveCycle())
        assertSame(true, true.resolveCycle())
        assertSame("abc", "abc".resolveCycle())
        listOf("a").let { assertSame(it, it.resolveCycle()) }
        mapOf("a" to 1).let { assertSame(it, it.resolveCycle()) }

        val list = arrayListOf<Any?>("a", "b")
        list.add(list)
        assertEquals(
            arrayListOf<Any?>("a", "b", arrayListOf<Any?>("a", "b", arrayListOf<Any?>())),
            list.resolveCycle()
        )

        val map = hashMapOf<Any?, Any?>("a" to 1, "b" to 2)
        map["c"] = map
        assertEquals(
            hashMapOf<Any?, Any?>(
                "a" to 1,
                "b" to 2,
                "c" to hashMapOf<Any?, Any?>("a" to 1, "b" to 2, "c" to hashMapOf<Any?, Any?>())
            ), map.resolveCycle()
        )
    }

    @Test
    fun testCopy() {
        assertEquals(null, null.copy())
        assertSame(1, 1.copy())
        assertSame(true, true.copy())
        assertSame("abc", "abc".copy())

        assertEquals(arrayListOf<String>(), listOf<String>().copy())
        assertEquals(arrayListOf<String>(), arrayListOf<String>().copy())
        assertEquals(arrayListOf("a"), listOf("a").copy())
        assertEquals(arrayListOf("a"), arrayListOf("a").copy())
        assertEquals(arrayListOf("a", "b"), listOf("a", "b").copy())
        assertEquals(arrayListOf("a", "b"), arrayListOf("a", "b").copy())

        assertEquals(hashMapOf<String, String>(), mapOf<String, String>().copy())
        assertEquals(hashMapOf<String, String>(), hashMapOf<String, String>().copy())
        assertEquals(hashMapOf("a" to "b"), mapOf("a" to "b").copy())
        assertEquals(
            hashMapOf("a" to "b", "c" to null, null to "d"),
            mapOf("a" to "b", "c" to null, null to "d").copy()
        )
        assertEquals(hashMapOf("a" to "b"), hashMapOf("a" to "b").copy())
        assertEquals(
            hashMapOf("a" to "b", "c" to null, null to "d"),
            hashMapOf("a" to "b", "c" to null, null to "d").copy()
        )
    }


}

private class AnyKitTestPoint(
    var x: Int,
    private var y: Int,
    protected var z: Int
) {

    fun publicX(): Int {
        return x
    }

    fun updateX(x: Int): Int {
        val old = this.x
        this.x = x
        return old
    }

    private fun privateY(): Int {
        return y
    }

    private fun updateY(y: Int) {
        this.y = y
    }

    fun getY(): Int {
        return this.y
    }

    protected fun protectedXY(): String {
        return "$x,$y"
    }


    protected fun updateXYZ(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    override fun toString(): String {
        return "{$x, $y, $z}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnyKitTestPoint

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    companion object {
        fun publicPoint(): AnyKitTestPoint {
            return AnyKitTestPoint(0, 0, 0)
        }

        private fun privatePoint(): AnyKitTestPoint {
            return AnyKitTestPoint(1, 1, 1)
        }

        protected fun protectedPoint(): AnyKitTestPoint {
            return AnyKitTestPoint(2, 2, 2)
        }
    }

}