package com.itangcent.common.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test class for [NumberUtils]
 */
class NumberUtilsTest {

    /**
     * Tests the fixDouble method which corrects floating-point precision issues in Double values.
     * 
     * This test verifies that:
     * - Integer values remain unchanged
     * - Values very close to integers are rounded to integers
     * - Simple decimal values are preserved
     * - Values with unnecessary trailing digits are simplified
     * - Negative values are handled correctly
     * - Very small and very large values are processed appropriately
     * - Values that should retain their precision do so
     */
    @Test
    fun testFixDouble() {
        // Test integers and near-integers
        assertEquals(5.0, NumberUtils.fixDouble(5.0))
        assertEquals(5.0, NumberUtils.fixDouble(5.0000001))
        assertEquals(5.0, NumberUtils.fixDouble(4.9999999))

        // Test simple decimal values
        assertEquals(5.5, NumberUtils.fixDouble(5.5))
        assertEquals(5.25, NumberUtils.fixDouble(5.25))

        // Test values with unnecessary trailing digits (the main issue)
        assertEquals(4.4, NumberUtils.fixDouble(4.400000095367432))
        assertEquals(3.14, NumberUtils.fixDouble(3.140000104904175))
        assertEquals(2.71, NumberUtils.fixDouble(2.7100000381469727))
        assertEquals(23.1, NumberUtils.fixDouble(23.099998))

        // Test negative values
        assertEquals(-5.0, NumberUtils.fixDouble(-5.0))
        assertEquals(-5.0, NumberUtils.fixDouble(-5.0000001))
        assertEquals(-3.14, NumberUtils.fixDouble(-3.140000104904175))

        // Test very small values
        assertEquals(0.01, NumberUtils.fixDouble(0.01))
        assertEquals(0.001, NumberUtils.fixDouble(0.001))
        assertEquals(0.0001, NumberUtils.fixDouble(0.0001))

        // Test very large values
        assertEquals(1000000.0, NumberUtils.fixDouble(1000000.0))
        assertEquals(1000000.5, NumberUtils.fixDouble(1000000.5))

        // Test values that should retain precision
        assertEquals(3.14159, NumberUtils.fixDouble(3.14159))
        assertEquals(2.71828, NumberUtils.fixDouble(2.71828))
    }

    /**
     * Tests the fixFloat method which corrects floating-point precision issues in Float values.
     * 
     * This test verifies that:
     * - Integer values remain unchanged
     * - Values very close to integers are rounded to integers
     * - Simple decimal values are preserved
     * - Values with unnecessary trailing digits are simplified
     * - Negative values are handled correctly
     * - Very small and very large values are processed appropriately
     */
    @Test
    fun testFixFloat() {
        // Test integers and near-integers
        assertEquals(5.0f, NumberUtils.fixFloat(5.0f))
        assertEquals(5.0f, NumberUtils.fixFloat(5.0000001f))
        assertEquals(5.0f, NumberUtils.fixFloat(4.9999999f))

        // Test simple decimal values
        assertEquals(5.5f, NumberUtils.fixFloat(5.5f))
        assertEquals(5.25f, NumberUtils.fixFloat(5.25f))

        // Test values with unnecessary trailing digits (the main issue)
        assertEquals(4.4f, NumberUtils.fixFloat(4.400000095367432f))
        assertEquals(3.14f, NumberUtils.fixFloat(3.140000104904175f))
        assertEquals(2.71f, NumberUtils.fixFloat(2.7100000381469727f))
        assertEquals(23.1f, NumberUtils.fixFloat(23.099998f))

        // Test negative values
        assertEquals(-5.0f, NumberUtils.fixFloat(-5.0f))
        assertEquals(-5.0f, NumberUtils.fixFloat(-5.0000001f))
        assertEquals(-3.14f, NumberUtils.fixFloat(-3.140000104904175f))

        // Test very small values
        assertEquals(0.01f, NumberUtils.fixFloat(0.01f))
        assertEquals(0.001f, NumberUtils.fixFloat(0.001f))
        assertEquals(0.0001f, NumberUtils.fixFloat(0.0001f))

        // Test very large values
        assertEquals(1000000.0f, NumberUtils.fixFloat(1000000.0f))
        assertEquals(1000000.5f, NumberUtils.fixFloat(1000000.5f))
    }
}