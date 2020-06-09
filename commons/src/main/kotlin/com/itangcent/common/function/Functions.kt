package com.itangcent.common.function

import java.util.*
import java.util.function.Function
import java.util.function.Predicate

object Functions {

    fun <T> from(predicate: Predicate<T>): java.util.function.Function<T, Boolean> {
        return Function { it -> predicate.test(it) }
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

}
