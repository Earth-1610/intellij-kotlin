package com.itangcent.test

import com.itangcent.common.concurrent.AQSCountLatch
import com.itangcent.common.utils.ConcurrentExtensible
import com.itangcent.common.utils.Extensible
import com.itangcent.common.utils.SimpleExtensible
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test case for [Extensible]
 */
class ExtensibleTest {

    @ParameterizedTest
    @ValueSource(classes = [SimpleExtensible::class, ConcurrentExtensible::class])
    fun testExtensible(cls: Class<Extensible>) {
        val extensible = cls.newInstance() as Extensible//{}
        assertFalse(extensible.hasExt("a"))
        assertFalse(extensible.hasAnyExt("a", "b"))
        extensible.setExt("b", "1")//{b:1}
        assertFalse(extensible.hasExt("a"))
        assertTrue(extensible.hasAnyExt("a", "b"))
        assertEquals(null, extensible.getExt("a"))
        assertEquals("1", extensible.getExt<Any>("b"))
        assertEquals(mapOf("b" to "1"), extensible.exts())
    }

    @Test
    fun testConcurrent() {
        val extensible = ConcurrentExtensible()
        val threadPool = Executors.newFixedThreadPool(10)
        val aqs = AQSCountLatch()
        for (i in 0..10) {
            aqs.down()
            threadPool.submit {
                try {
                    for (j in 0..10) {
                        extensible.setExt((i + j).toString(), RandomUtils.nextInt(0, 9999))
                        Thread.sleep(RandomUtils.nextLong(0, 10))
                    }
                } finally {
                    aqs.up()
                }
            }
        }
        threadPool.shutdown()
        aqs.waitFor(2000)
        assertTrue(extensible.hasAnyExt("1"))
        assertEquals(21, extensible.exts()!!.size)

    }
}