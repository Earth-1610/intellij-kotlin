package com.itangcent.common.utils

import java.util.stream.Stream
import java.util.stream.StreamSupport

object Iterables {
    fun <T> asIterable(generator: () -> T?): Iterable<T> {
        return IterableFromGenerator(generator)
    }
}

fun <T> Iterable<T>.asGenerator(): () -> T? {
    val iterator = this.iterator()
    return {
        if (iterator.hasNext()) {
            iterator.next()
        } else {
            null
        }
    }
}

fun <T> Iterable<T>.asStream(): Stream<T> {
    return StreamSupport.stream(this.spliterator(), false)
}

fun <T, R> Iterator<T>.map(transform: (T) -> R): Iterator<R> {
    return IteratorTransformed(this, transform)
}

internal class IterableTransformed<T, R>(
    private val iterable: Iterable<T>,
    private val transform: (T) -> R
) : Iterable<R> {

    override fun iterator(): Iterator<R> {
        return IteratorTransformed(iterable.iterator(), transform)
    }
}

internal class IteratorTransformed<T, R>(
    private val iterator: Iterator<T>,
    private val transform: (T) -> R
) : Iterator<R> {

    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }

    override fun next(): R {
        return transform(iterator.next())
    }
}

internal class IterableFromGenerator<T>(private val generator: () -> T?) : Iterable<T> {

    override fun iterator(): Iterator<T> {
        return IteratorFromGenerator(generator)
    }
}

internal class IteratorFromGenerator<T>(private val generator: () -> T?) : Iterator<T> {

    private var next: T? = null

    override fun hasNext(): Boolean {
        if (next != null) {
            return true
        }
        next = generator()
        return next != null
    }

    override fun next(): T {
        val ret = next
        next = null
        return ret!!
    }
}