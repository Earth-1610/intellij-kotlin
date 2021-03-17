package com.itangcent.common

import kotlin.test.assertEquals


object ModelBaseTest {
    fun equal(one: Any, other: Any) {
        assertEquals(one, other)
        assertEquals(one.hashCode(), other.hashCode())
        assertEquals(one.toString(), other.toString())
    }
}