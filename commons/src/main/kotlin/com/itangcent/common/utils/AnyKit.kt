package com.itangcent.common.utils

import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.LinkedHashMap
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty


/**
 * get object property value by name
 */
fun Any.getPropertyValue(propertyName: String): Any? {
    return getClassPropertyValueByName(this, propertyName)
}

/**
 * change object property value by name
 */
fun Any.changePropertyValue(propertyName: String, newValue: Any?) {
    changeClassPropertyValueByName(this, propertyName, newValue)
}

/**
 * change object property value by name
 */
fun <R> Any.changePropertyValueByPropertyReference(kProperty: KProperty<R>, newValue: Any?) {
    changeClassPropertyValue(this, kProperty, newValue)
}

/**
 * invoke a method through object by method name
 *
 */
fun Any.invokeMethod(methodName: String, vararg args: Any?): Any? {
    return invokeClassMethodByMethodName(this, methodName, *args)
}

/**
 * invoke a static method through kclass by method name
 */
fun KClass<*>.invokeStaticMethod(methodName: String, vararg args: Any?): Any? {
    return invokeStaticClassMethodByMethodName(this, methodName, *args)
}

/**
 * change current this property value
 */
fun <R> KProperty<R>.changeValue(thisObj: Any, newValue: Any?) {
    changeClassPropertyValue(thisObj, this, newValue)
}

/**
 * get other package level property value by other package level property name which is in the same kotlin file
 */
fun <R> KProperty<R>.packageLevelGetPropertyValueByName(otherPropertyName: String): Any? {
    return getTopPropertyValueByName(this as CallableReference, otherPropertyName)
}

/**
 * get other package level property value by other package level property name which is in the same kotlin file
 */
fun <R> KFunction<R>.packageLevelGetPropertyValueByName(otherPropertyName: String): Any? {
    return getTopPropertyValueByName(this as CallableReference, otherPropertyName)
}

/**
 * change package level property value
 */
fun <R> KProperty<R>.packageLevelChangePropertyValue(newValue: Any?) {
    changeTopPropertyValue(this, newValue)
}

/**
 * change other package level property value by other package level property name which is in the same kotlin file
 */
fun <R> KProperty<R>.packageLevelChangeOtherPropertyValueByName(otherPropertyName: String, newValue: Any?) {
    changeTopPropertyValueByName(this as CallableReference, otherPropertyName, newValue)
}

/**
 * change other package level property value by other package level property name which is in the same kotlin file
 */
fun <R> KFunction<R>.packageLevelChangeOtherPropertyValueByName(otherPropertyName: String, newValue: Any?) {
    changeTopPropertyValueByName(this as CallableReference, otherPropertyName, newValue)
}

/**
 * invoke package level method by name which is in the same kotlin file
 */
fun <R> KProperty<R>.packageLevelInvokeMethodByName(methodName: String, vararg args: Any?): Any? {
    return invokeTopMethodByMethodName(this as CallableReference, methodName, *args)
}

/**
 * invoke package level method by name which is in the same kotlin file
 */
fun <R> KFunction<R>.packageLevelInvokeMethodByName(methodName: String, vararg args: Any?): Any? {
    return invokeTopMethodByMethodName(this as CallableReference, methodName, *args)
}

@Suppress("UNCHECKED_CAST")
fun <Input, Out : Any> Input?.cast(out: KClass<Out>): Out? {
    if (this == null) {
        return null
    }

    if (out.isInstance(this)) {
        return this as Out
    }

    return null
}

fun Any?.anyIsNullOrEmpty(): Boolean {
    return when {
        this == null -> true
        this is String && this.isEmpty() -> true
        this is Array<*> && this.isEmpty() -> true
        this is Collection<*> && this.isEmpty() -> true
        this is Map<*, *> && this.isEmpty() -> true
        else -> false
    }
}

fun Any?.anyIsNullOrBlank(): Boolean {
    return when {
        this == null -> true
        this is String && this.isBlank() -> true
        else -> false
    }
}

fun Any?.asInt(): Int? {
    return when {
        this == null -> null
        this is Int -> this
        this is Boolean -> {
            return when {
                this -> 1
                else -> 0
            }
        }
        this is Number -> this.toInt()
        this is String -> this.toIntOrNull()
        else -> null
    }
}

fun Any?.asLong(): Long? {
    return when {
        this == null -> null
        this is Long -> this
        this is Boolean -> {
            return when {
                this -> 1
                else -> 0
            }
        }
        this is Number -> this.toLong()
        this is String -> this.toLongOrNull()
        else -> null
    }
}

fun Any?.asFloat(): Float? {
    return when {
        this == null -> null
        this is Float -> this
        this is Boolean -> {
            return when {
                this -> 1f
                else -> 0f
            }
        }
        this is Number -> this.toFloat()
        this is String -> this.toFloatOrNull()
        else -> null
    }
}

fun Any?.asDouble(): Double? {
    return when {
        this == null -> null
        this is Double -> this
        this is Boolean -> {
            return when {
                this -> 1.0
                else -> 0.0
            }
        }
        this is Number -> this.toDouble()
        this is String -> this.toDoubleOrNull()
        else -> null
    }
}

fun Any?.asBool(): Boolean? {
    return when {
        this == null -> null
        this is Boolean -> this
        this is Number -> this.toInt() == 1
        this is String -> this == "true" || this == "1"
        else -> null
    }
}

fun Any?.resolveCycle(): Any? {
    when (this) {
        null -> return null
        is Map<*, *>, is Collection<*> -> {
            if (!this.collectValues()) {
                return this.copy()
            }
            return this
        }
        else -> return this
    }
}

fun Any?.copy(): Any? {
    return when (this) {
        null -> null
        is Map<*, *> -> this.copy(ValuePresence())
        is Collection<*> -> this.copy(ValuePresence())
        is Cloneable -> try {
            return this.invokeMethod("clone")
        } catch (e: Exception) {
        }
        else -> this
    }
}

private fun Any?.copy(values: ValuePresence): Any? {

    when (this) {
        null -> return null
        is Map<*, *> -> {
            if (!values.add(this)) return emptyMap<Any?, Any?>()
            val newCopy: HashMap<Any?, Any?> = LinkedHashMap()
            this.forEach { (key, value) ->
                newCopy[key] = value.copy(values)
            }
            values.pop()
            return newCopy
        }
        is Collection<*> -> {
            if (!values.add(this)) return ArrayList<Any?>()
            val newCopy: ArrayList<Any?> = ArrayList()
            this.forEach { value ->
                value.copy(values)?.let { newCopy.add(it) }
            }
            values.pop()
            return newCopy
        }
        else -> return this
    }
}

private fun Any?.collectValues(): Boolean {
    return when (this) {
        null -> true
        is Map<*, *> -> this.collectValues(ValuePresence())
        is Collection<*> -> this.collectValues(ValuePresence())
        else -> true
    }
}

private fun Any?.collectValues(values: ValuePresence): Boolean {

    if (this == null) return true

    if (this is Map<*, *>) {
        if (!values.add(this)) return false

        for (value in this.values) {
            if (!value.collectValues(values)) {
                return false
            }
        }
        values.pop()

        return true
    }

    if (this is Collection<*>) {
        if (!values.add(this)) return false
        for (value in this) {
            if (!value.collectValues(values))
                return false
        }

        values.pop()
    }
    return true
}

private class ValuePresence {

    private val values: ArrayDeque<Any?> = ArrayDeque()

    fun add(one: Any?): Boolean {
        if (one == null) {
            values.add(one)
            return true
        }

        if (values.count { it === one } > 1) {
            return false
        }
        values.add(one)
        return true
    }

    fun pop() {
        values.removeLastOrNull()
    }

    fun clear() {
        values.clear()
    }
}
