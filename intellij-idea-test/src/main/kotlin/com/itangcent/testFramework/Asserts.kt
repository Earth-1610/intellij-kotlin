package com.itangcent.testFramework

import junit.framework.TestCase.*


/**
 * Helper function to assert map equality with special handling for numeric types
 */
fun assertMapEquals(expected: Map<String, Any?>, actual: Map<String, Any?>?) {

    val expectedStr = expected.entries.joinToString(", ") { (key, value) -> "$key: $value" }
    val actualStr = actual?.entries?.joinToString(", ") { (key, value) -> "$key: $value" }
    val failedComparisonMessage = "Expected: $expectedStr\nActual: $actualStr"

    assertNotNull("Actual map should not be null, $failedComparisonMessage", actual)
    assertEquals("Maps should have the same size, $failedComparisonMessage", expected.size, actual!!.size)

    for ((key, expectedValue) in expected) {
        assertTrue("Actual map should contain key: $key, $failedComparisonMessage", actual.containsKey(key))
        val actualValue = actual[key]

        when {
            // Special handling for numeric comparisons
            expectedValue is Number && actualValue is Number -> {
                assertEquals(
                    "Values for key '$key' should be equal, $failedComparisonMessage",
                    expectedValue.toDouble(), actualValue.toDouble(), 0.0001
                )
            }
            // For other types, use regular equality
            else -> {
                assertEquals(
                    "Values for key '$key' should be equal, $failedComparisonMessage",
                    expectedValue,
                    actualValue
                )
            }
        }
    }
}
