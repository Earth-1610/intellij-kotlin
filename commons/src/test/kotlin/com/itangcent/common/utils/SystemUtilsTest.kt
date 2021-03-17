package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class SystemUtilsTest {

    @Test
    fun test() {
        assertNotNull(SystemUtils.userName)
        assertNotNull(SystemUtils.userHome)
        assertNotNull(SystemUtils.newLine())
        assertNotNull(SystemUtils.isWindows)
    }
}