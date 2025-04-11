package com.itangcent.common.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate

/**
 * Test class for [Functions]
 */
class FunctionsTest {

    /**
     * Tests the from function that converts a Predicate to a Function
     */
    @Test
    fun testFrom() {
        val isEven = Predicate<Int> { it % 2 == 0 }
        val isEvenFunction = Functions.from(isEven)
        
        assertTrue(isEvenFunction.apply(2))
        assertFalse(isEvenFunction.apply(3))
        assertTrue(isEvenFunction.apply(0))
        assertFalse(isEvenFunction.apply(-1))
        assertTrue(isEvenFunction.apply(-2))
    }
    
    /**
     * Tests the nullAs function that returns a default value for null inputs
     */
    @Test
    fun testNullAs() {
        val defaultString = "default"
        val nullAsFunction = Functions.nullAs(defaultString)
        
        assertEquals("test", nullAsFunction.apply("test"))
        assertEquals(defaultString, nullAsFunction.apply(null))
    }
    
    /**
     * Tests the nullAs function that applies a function to non-null values
     * and returns a default value for null inputs
     */
    @Test
    fun testNullAsWithFunction() {
        val transform = java.util.function.Function<String?, Int> { it?.length ?: 0 }
        val defaultValue = -1
        val nullAsFunction = Functions.nullAs(transform, defaultValue)
        
        assertEquals(4, nullAsFunction.apply("test"))
        assertEquals(0, nullAsFunction.apply(""))
        assertEquals(defaultValue, nullAsFunction.apply(null))
    }
    
    /**
     * Tests the exactlyOnce function that ensures a function with a parameter
     * is executed at most once
     */
    @Test
    fun testExactlyOnceWithParam() {
        val counter = AtomicInteger(0)
        val function: (String) -> Unit = { _ -> counter.incrementAndGet() }
        val exactlyOnceFunction = Functions.exactlyOnce(function)
        
        assertEquals(0, counter.get())
        exactlyOnceFunction("first call")
        assertEquals(1, counter.get())
        exactlyOnceFunction("second call")
        assertEquals(1, counter.get())
        exactlyOnceFunction("third call")
        assertEquals(1, counter.get())
    }
    
    /**
     * Tests the exactlyOnce function that ensures a function without parameters
     * is executed at most once
     */
    @Test
    fun testExactlyOnceNoParam() {
        val counter = AtomicInteger(0)
        val function: () -> Unit = { counter.incrementAndGet() }
        val exactlyOnceFunction = Functions.exactlyOnce(function)
        
        assertEquals(0, counter.get())
        exactlyOnceFunction()
        assertEquals(1, counter.get())
        exactlyOnceFunction()
        assertEquals(1, counter.get())
        exactlyOnceFunction()
        assertEquals(1, counter.get())
    }
    
    /**
     * Tests the map extension function for functions that return nullable values
     */
    @Test
    fun testMapExtension() {
        val stringSupplier = { "test" }
        val lengthMapper = { s: String? -> s?.length }
        
        val mappedFunction = stringSupplier.map(lengthMapper)
        assertEquals(4, mappedFunction())
        
        val nullSupplier = { null as String? }
        val mappedNullFunction = nullSupplier.map(lengthMapper)
        assertNull(mappedNullFunction())
    }
    
    /**
     * Tests the nullAs extension function for Function instances
     */
    @Test
    fun testNullAsExtension() {
        val transform = java.util.function.Function<String?, Int> { it?.length ?: 0 }
        val defaultValue = -1
        val nullAsFunction = transform.nullAs(defaultValue)
        
        assertEquals(4, nullAsFunction.apply("test"))
        assertEquals(0, nullAsFunction.apply(""))
        assertEquals(defaultValue, nullAsFunction.apply(null))
    }
    
    /**
     * Tests the exactlyOnce extension function for functions with a parameter
     */
    @Test
    fun testExactlyOnceExtensionWithParam() {
        val counter = AtomicInteger(0)
        val function: (String) -> Unit = { _ -> counter.incrementAndGet() }
        val exactlyOnceFunction = function.exactlyOnce()
        
        assertEquals(0, counter.get())
        exactlyOnceFunction("first call")
        assertEquals(1, counter.get())
        exactlyOnceFunction("second call")
        assertEquals(1, counter.get())
        exactlyOnceFunction("third call")
        assertEquals(1, counter.get())
    }
    
    /**
     * Tests the exactlyOnce extension function for functions without parameters
     */
    @Test
    fun testExactlyOnceExtensionNoParam() {
        val counter = AtomicInteger(0)
        val function: () -> Unit = { counter.incrementAndGet() }
        val exactlyOnceFunction = function.exactlyOnce()
        
        assertEquals(0, counter.get())
        exactlyOnceFunction()
        assertEquals(1, counter.get())
        exactlyOnceFunction()
        assertEquals(1, counter.get())
        exactlyOnceFunction()
        assertEquals(1, counter.get())
    }
}