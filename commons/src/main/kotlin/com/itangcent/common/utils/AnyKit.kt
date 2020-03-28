package com.itangcent.common.utils

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
fun Any.changePropertyValueIgnoreItsType(propertyName: String, newValue: Any?) {
    changeClassPropertyValueByNameIgnoreType(this, propertyName, newValue)
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

fun Any?.isNullOrEmpty(): Boolean {
    return when {
        this == null -> true
        this is String && this.isEmpty() -> true
        this is Array<*> && this.isEmpty() -> true
        this is Collection<*> && this.isEmpty() -> true
        this is Map<*, *> && this.isEmpty() -> true
        else -> false
    }
}

fun Any?.isNullOrBlank(): Boolean {
    return when {
        this == null -> true
        this is String && this.isBlank() -> true
        else -> false
    }
}

fun Any?.toInt(): Int? {
    return when {
        this == null -> null
        this is Int -> this
        this is Number -> this.toInt()
        this is String -> this.toIntOrNull()
        else -> null
    }
}

fun String?.notNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}