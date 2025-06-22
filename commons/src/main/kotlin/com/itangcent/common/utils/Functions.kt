package com.itangcent.common.utils

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import java.util.function.Predicate

object Functions {
    fun <T> from(predicate: Predicate<T>): java.util.function.Function<T, Boolean> {
        return Function { predicate.test(it) }
    }

    /**
     * Creates a  `Function` that convert null as the given default value
     *
     * @param <T>        the type of the input and output of the function
     * @param defaultVal the result which returned if value is null
     * @return the function result or `null` if exception was thrown
     * @throws NullPointerException if `defaultVal` is null
    </T> */
    fun <T> nullAs(defaultVal: T): Function<T?, T> {
        Objects.requireNonNull(defaultVal)
        return Function { value -> value ?: defaultVal }
    }

    /**
     * Creates a  `Function` that convert null as the given default value
     *
     * @param <T>        the type of the input and output of the function
     * @param function   the result which used if value is not null
     * @param defaultVal the result which returned if value is null
     * @return the function result or `null` if exception was thrown
     * @throws NullPointerException if `defaultVal` is null
    </T> */
    fun <T, R> nullAs(function: Function<T?, R>, defaultVal: R): Function<T?, R> {
        Objects.requireNonNull(function)
        return Function { value -> if (value == null) defaultVal else function.apply(value) }
    }

    /**
     * Converts a function to an "exactly once" function that will only execute the first time it's called.
     * Subsequent calls will be ignored.
     *
     * @param <T> the type of the function parameter
     * @return a function that executes the original function at most once
     */
    fun <T> exactlyOnce(function: (T) -> Unit): (T) -> Unit {
        val executed = AtomicBoolean(false)
        return { param ->
            if (executed.compareAndSet(false, true)) {
                function(param)
            }
        }
    }

    /**
     * Converts a function with no parameters to an "exactly once" function that will only execute
     * the first time it's called. Subsequent calls will be ignored.
     *
     * @return a function that executes the original function at most once
     */
    fun exactlyOnce(function: () -> Unit): () -> Unit {
        val executed = AtomicBoolean(false)
        return {
            if (executed.compareAndSet(false, true)) {
                function()
            }
        }
    }
}

fun <T, R> (() -> T?).map(transform: (T?) -> R?): () -> R? {
    return {
        transform(this())
    }
}

/**
 * Extension function that applies a function to a non-null value or returns a default value if it's null
 */
fun <T, R> Function<T?, R>.nullAs(defaultVal: R): Function<T?, R> {
    return Functions.nullAs(this, defaultVal)
}

/**
 * Extension function that converts a function to an "exactly once" function
 */
fun <T> ((T) -> Unit).exactlyOnce(): (T) -> Unit {
    return Functions.exactlyOnce(this)
}

/**
 * Extension function that converts a function with no parameters to an "exactly once" function
 */
fun (() -> Unit).exactlyOnce(): () -> Unit {
    return Functions.exactlyOnce(this)
}
