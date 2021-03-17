package com.itangcent.common.utils

import com.itangcent.common.utils.ClassHelper.getDeclaredFieldInHierarchy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Test case of [ClassHelper]
 */
class ClassHelperTest {

    @Test
    fun testIsArray() {
        assertFalse(ClassHelper.isArray(null))
        assertFalse(ClassHelper.isArray(1))
        assertFalse(ClassHelper.isArray(1L))
        assertFalse(ClassHelper.isArray("str"))
        assertFalse(ClassHelper.isArray(Object()))
        assertTrue(ClassHelper.isArray(emptyArray<Any?>()))
        assertFalse(ClassHelper.isArray(emptyList<Any?>()))
        assertTrue(ClassHelper.isArray(arrayOf(1)))
        assertFalse(ClassHelper.isArray(listOf("str")))
    }

    @Test
    fun testIsCollection() {
        assertFalse(ClassHelper.isCollection(null))
        assertFalse(ClassHelper.isCollection(1))
        assertFalse(ClassHelper.isCollection(1L))
        assertFalse(ClassHelper.isCollection("str"))
        assertFalse(ClassHelper.isCollection(Object()))
        assertFalse(ClassHelper.isCollection(emptyArray<Any?>()))
        assertTrue(ClassHelper.isCollection(emptyList<Any?>()))
        assertFalse(ClassHelper.isCollection(arrayOf(1)))
        assertTrue(ClassHelper.isCollection(listOf("str")))
    }

    @Test
    fun testIsArrayOrCollection() {
        assertFalse(ClassHelper.isArrayOrCollection(null))
        assertFalse(ClassHelper.isArrayOrCollection(1))
        assertFalse(ClassHelper.isArrayOrCollection(1L))
        assertFalse(ClassHelper.isArrayOrCollection("str"))
        assertFalse(ClassHelper.isArrayOrCollection(Object()))
        assertTrue(ClassHelper.isArrayOrCollection(emptyArray<Any?>()))
        assertTrue(ClassHelper.isArrayOrCollection(emptyList<Any?>()))
        assertTrue(ClassHelper.isArrayOrCollection(arrayOf(1)))
        assertTrue(ClassHelper.isArrayOrCollection(listOf("str")))
    }

    @Test
    fun testNewInstance() {
        assertEquals("A", ClassHelper.newInstance(ClassHelperTestA::class).toString())
        assertThrows<IllegalArgumentException> { ClassHelper.newInstance(ClassHelperTestB::class, 1) }
        assertEquals("hello world", ClassHelper.newInstance(ClassHelperTestB::class, "hello world").toString())
        assertEquals("empty", ClassHelper.newInstance(ClassHelperTestC::class).toString())
        assertEquals("1", ClassHelper.newInstance(ClassHelperTestC::class, 1).toString())
        assertEquals("1,2", ClassHelper.newInstance(ClassHelperTestC::class, 1, 2).toString())
        assertThrows<IllegalArgumentException> { ClassHelper.newInstance(ClassHelperTestC::class, "x", 1) }
        assertEquals("hello world", ClassHelper.newInstance(ClassHelperTestC::class, "hello world").toString())
        assertEquals("hello world", ClassHelperTestC::class.newInstance("hello world").toString())
    }

    @Test
    fun testGetDeclaredFieldInHierarchy() {
        assertEquals(
            ClassHelperTestC::class.java.getDeclaredFieldInHierarchy("str"),
            ClassHelperTestD::class.java.getDeclaredFieldInHierarchy("str")
        )
        assertNull(ClassHelperTestC::class.java.getDeclaredFieldInHierarchy("aaa"))
        assertNull(ClassHelperTestA::class.java.getDeclaredFieldInHierarchy("str"))
    }

    @Test
    fun testCheckedReflection() {
        assertEquals("s", ClassHelper.checkedReflection({ "s" }, { "s" }))
        assertThrows<IllegalArgumentException> {
            ClassHelper.checkedReflection({ throw InvocationTargetException(IllegalArgumentException()) }, { "s" })
        }
        assertEquals("s", ClassHelper.checkedReflection({ throw ReflectiveOperationException() }, { "s" }))
        assertEquals("s", ClassHelper.checkedReflection({ throw IllegalArgumentException() }, { "s" }))
    }
}

class ClassHelperTestA {
    override fun toString(): String {
        return "A"
    }
}

class ClassHelperTestB(private val str: String) {

    override fun toString(): String {
        return str
    }
}

open class ClassHelperTestC {
    protected val str: String

    constructor() {
        this.str = "empty"
    }

    constructor(i: Int) {
        this.str = i.toString()
    }

    constructor(x: Int, y: Int) {
        this.str = "$x,$y"
    }

    constructor(str: String) {
        this.str = str
    }

    override fun toString(): String {
        return str
    }
}

class ClassHelperTestD(val x: Long) : ClassHelperTestC() {
    override fun toString(): String {
        return "$x-$str"
    }
}