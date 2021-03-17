package com.itangcent.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger
import java.util.*
import kotlin.test.assertNotNull

/**
 * Test case of [JavaClassHelper]
 */
class JavaClassHelperTest {

    @Test
    fun testNewInstance() {
        assertNotNull(JavaClassHelper.newInstance(Object::class.java))
        assertThrows<RuntimeException> { JavaClassHelper.newInstance(BigInteger::class.java) }

        assertNotNull(JavaClassHelper.newInstance(String::class.java, "a" to String::class.java))
        assertNotNull(JavaClassHelper.newInstance(ArrayList::class.java, 10 to Int::class.java))
        assertThrows<RuntimeException> {
            JavaClassHelper.newInstance(
                ArrayList::class.java,
                "hello" to String::class.java
            )
        }
    }
}