package com.itangcent.intellij.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppropriateStringMatcherTest {

    @Test
    fun findAppropriate() {
        assertEquals(
            "type",
            AppropriateStringMatcher.findAppropriate("type", listOf("type", "name", "status"))
        )
        assertEquals(
            "type",
            AppropriateStringMatcher.findAppropriate("userType", listOf("type", "name", "status"))
        )
        assertEquals(
            "name",
            AppropriateStringMatcher.findAppropriate("user_name", listOf("type", "name", "status"))
        )
        assertEquals(
            "status",
            AppropriateStringMatcher.findAppropriate("userStatusList", listOf("type", "name", "status"))
        )
        assertEquals(
            null,
            AppropriateStringMatcher.findAppropriate("_type_or_status_", listOf("type", "name", "status"))
        )
    }
}