package com.itangcent.common.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Utility class for Numbers
 */
object NumberUtils {

    /**
     * Epsilon value used for floating point comparisons.
     * Defines what we consider "close enough" when comparing floating point values.
     *
     * This small threshold helps determine when two floating-point values should be
     * considered equal, accounting for representation errors.
     */
    private const val EPSILON = 0.000001f

    /**
     * A slightly larger epsilon for cases where we want to detect values that are
     * very close to common decimal representations (like .1, .2, etc.)
     */
    private const val DECIMAL_EPSILON = 0.0001

    /**
     * Fixes floating point precision issues in Float values.
     *
     * This method attempts to simplify float representations by:
     * 1. Rounding values very close to integers to those integers
     * 2. Removing unnecessary trailing digits from decimal representations
     * 3. Finding the simplest representation within the EPSILON threshold
     *
     * For example, converts 3.140000104904175f to 3.14f
     *
     * @param value The Float value to fix
     * @return A Float with precision issues fixed
     */
    fun fixFloat(value: Float): Float {
        return fixDouble(value.toDouble()).toFloat()
    }

    /**
     * Fixes floating point precision issues in Double values.
     *
     * This method attempts to simplify double representations by:
     * 1. Rounding values very close to integers to those integers
     * 2. Handling special cases like scientific notation and very small values
     * 3. Removing unnecessary trailing digits from decimal representations
     * 4. Finding the simplest representation within the EPSILON threshold
     *
     * For example, converts 4.400000095367432 to 4.4 and 23.099998 to 23.1
     *
     * @param value The Double value to fix
     * @return A Double with precision issues fixed
     */
    fun fixDouble(value: Double): Double {
        // Special case for zero
        if (value == 0.0) {
            return 0.0
        }

        // Get string representation to analyze
        val stringValue = value.toString()

        // Check for patterns that indicate floating point precision issues
        if (!stringValue.contains("0000") && !stringValue.contains("9999")) {
            return value
        }

        // For scientific notation or very small values, handle carefully
        if (stringValue.contains("E") || abs(value) < DECIMAL_EPSILON) {
            try {
                // Create BigDecimal from string to preserve exact representation
                val bd = BigDecimal(stringValue)

                // For very small values, just return as is to avoid rounding to zero
                if (abs(value) < DECIMAL_EPSILON) {
                    return bd.toDouble()
                }

                // For scientific notation, convert to a reasonable scale
                return bd.toDouble()
            } catch (_: Exception) {
                return value
            }
        }

        // First check if it's very close to an integer
        val rounded = value.roundToLong()
        if (abs(value - rounded) < EPSILON) {
            return rounded.toDouble()
        }

        try {
            // Create BigDecimal from string to preserve exact representation
            val bd = BigDecimal(stringValue)

            // Check for common decimal patterns (like x.1, x.2, etc.)
            // This specifically helps with cases like 23.099998 -> 23.1
            val intPart = value.toLong()
            val fracPart = value - intPart

            // Check if the fractional part is very close to a common decimal
            for (i in 1..9) {
                val target = i / 10.0
                if (abs(fracPart - target) < DECIMAL_EPSILON) {
                    return intPart + target
                }
            }

            // Try to simplify the representation by removing trailing zeros
            var stripped = bd.stripTrailingZeros()

            // For numbers with reasonable scale, check if they can be simplified further
            if (stripped.scale() > 0) {
                // Try progressively smaller scales to find the simplest representation
                // that's still within our epsilon of the original value
                for (scale in minOf(stripped.scale(), 10) downTo 1) {
                    val simplified = bd.setScale(scale, RoundingMode.HALF_UP)

                    // Use epsilon for comparison
                    if (abs(simplified.toDouble() - value) > EPSILON) {
                        break
                    }
                    stripped = simplified;
                }
            }

            // If no simplification works well, use the stripped value
            return stripped.toDouble()
        } catch (_: Exception) {
            // If any error occurs, return the original value
            return value
        }
    }
}