package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SafeHashSetTest {

    @Test
    fun testSafeHashSet() {
        val set = SafeHashSet<Any>()
        assertTrue(set.add("1"))
        assertTrue(set.add(1))
        assertFalse(set.add(1))
        assertTrue(set.add(SafeHashSetTestModel(1)))
        assertTrue(set.add(SafeHashSetTestModel(2)))
        assertFalse(set.add(SafeHashSetTestModel(1)))
        assertFalse(set.add(SafeHashSetTestModel(2)))
    }

    @Test
    fun testReentrantSafeHashSet() {
        val set = ReentrantSafeHashSet<Any>(2)
        assertTrue(set.addElement("1"))
        assertTrue(set.addElement("1"))
        assertTrue(set.addElement(1))
        assertTrue(set.addElement(1))
        assertFalse(set.addElement(1))
        assertTrue(set.addElement(SafeHashSetTestModel(1)))
        assertTrue(set.addElement(SafeHashSetTestModel(1)))
        assertTrue(set.addElement(SafeHashSetTestModel(2)))
        assertTrue(set.addElement(SafeHashSetTestModel(2)))
        assertFalse(set.addElement(SafeHashSetTestModel(1)))
        assertFalse(set.addElement(SafeHashSetTestModel(2)))
    }
}

class SafeHashSetTestModel(val x: Int) {
    override fun hashCode(): Int {
        throw StackOverflowError("failed")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SafeHashSetTestModel

        if (x != other.x) return false

        return true
    }
}