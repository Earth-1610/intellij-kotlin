package com.itangcent.common.utils

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ExtensionKtTest {

    @Test
    fun safe() {
        org.junit.jupiter.api.assertDoesNotThrow { safe { throw RuntimeException() } }
        org.junit.jupiter.api.assertDoesNotThrow { safe(RuntimeException::class) { throw RuntimeException() } }
        assertThrows(RuntimeException::class.java) { safe(IllegalArgumentException::class) { throw RuntimeException() } }
        org.junit.jupiter.api.assertDoesNotThrow { safe(RuntimeException::class) { throw IllegalArgumentException() } }
        org.junit.jupiter.api.assertDoesNotThrow {
            safe(
                RuntimeException::class,
                IllegalArgumentException::class
            ) { throw IllegalArgumentException() }
        }
        assertThrows(RuntimeException::class.java) {
            safe(
                IllegalStateException::class,
                IllegalArgumentException::class
            ) { throw RuntimeException() }
        }
    }
}