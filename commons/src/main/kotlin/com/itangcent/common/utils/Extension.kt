package com.itangcent.common.utils

import kotlin.reflect.KClass


/**
 * Calls the specified function [block] and returns its result safely.
 * Not throw any exceptions.
 */
inline fun <R> safe(block: () -> R): R? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}


fun <R> safe(ignoreThrowable: KClass<*>, action: () -> R): R? {
    return try {
        action()
    } catch (e: Exception) {
        if (ignoreThrowable.isInstance(e)) {
            null
        } else {
            throw e
        }
    }
}

fun <R> safe(vararg ignoreThrowable: KClass<*>, action: () -> R): R? {
    return try {
        action()
    } catch (e: Exception) {
        for (throwable in ignoreThrowable) {
            if (throwable.isInstance(e)) {
                return null
            }
        }
        throw e
    }
}