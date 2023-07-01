package com.itangcent.common.utils

import com.itangcent.common.logger.Log
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference
import kotlin.jvm.internal.PropertyReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * change the property value with new value int the special KProperty in package level property ,not the property in class
 *
 */
fun <R> changeTopPropertyValue(property: KProperty<R>, newValue: R): Boolean =
    changePropertyValue(null, property, newValue)

/**
 * change the property value with new value int the special KProperty inside a  class level ,not the property not in any class
 */
fun <R> changeClassPropertyValue(classObj: Any, property: KProperty<R>, newValue: R): Boolean =
    changePropertyValue(classObj, property, newValue)

private fun <R> changePropertyValue(classObj: Any?, property: KProperty<R>, newValue: R): Boolean {
    val owner = (property as PropertyReference).owner
    val propertyName = property.name
    val containerClass: Class<*>
    try {
        containerClass =
            (owner!!::class.members as ArrayList).firstOrNull { it.name == "jClass" }?.call(owner) as Class<*>
    } catch (e: Exception) {
        throw IllegalArgumentException("No such property 'jClass'")
    }
    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredFields.forEach { field ->
            if (field.name == propertyName) {
                field.isAccessible = true
                removeFinalModifies(field)

                field.set(classObj, newValue)
                return true
            }
        }

        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }

    return false
}

/**
 * change the property value with new value int the special property name inside a class level ,not the property not in any class
 */
fun <R> changeClassPropertyValueByName(classObj: Any, propertyName: String, newValue: R): Boolean {
    val containerClass: Class<*> = classObj::class.java

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredFields.forEach { field ->
            if (field.name == propertyName) {
                field.isAccessible = true
                removeFinalModifies(field)

                field.set(classObj, newValue)
                return true
            }
        }

        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }
    return false
}

/**
 * change the property value with new value int the special Property name in package level property ,not the property in class
 */
fun changeTopPropertyValueByName(otherCallableReference: CallableReference, propertyName: String, newValue: Any?) {

    val owner = otherCallableReference.owner
    val containerClass: Class<*>
    try {
        containerClass =
            (owner!!::class.members as ArrayList).firstOrNull { it.name == "jClass" }?.call(owner) as Class<*>
    } catch (e: Exception) {
        throw IllegalArgumentException("No such property 'jClass'")
    }

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredFields.forEach { field ->
            if (field.name == propertyName) {
                field.isAccessible = true
                updateModifies(field) {
                    (it and Modifier.FINAL.inv()) and Modifier.PRIVATE.inv()
                }
                /**
                 * top property(package property) should be static in java level
                 * or throw an exception
                 * */
                val clazz = when (field.type) {
                    Int::class.javaPrimitiveType -> Class.forName("sun.reflect.UnsafeQualifiedStaticIntegerFieldAccessorImpl")
                    Long::class.javaPrimitiveType -> Class.forName("sun.reflect.UnsafeQualifiedStaticLongFieldAccessorImpl")
                    Double::class.javaPrimitiveType -> Class.forName("sun.reflect.UnsafeQualifiedStaticDoubleFieldAccessorImpl")
                    Float::class.javaPrimitiveType -> Class.forName("sun.reflect.UnsafeQualifiedStaticFloatFieldAccessorImpl")
                    Boolean::class.javaPrimitiveType -> Class.forName("sun.reflect.UnsafeQualifiedStaticBooleanFieldAccessorImpl")
                    else -> Class.forName("sun.reflect.UnsafeQualifiedStaticObjectFieldAccessorImpl")
                }
                val constructor = clazz.declaredConstructors[0]
                constructor.isAccessible = true

                val customAccess = constructor.newInstance(field, false)

                field.javaClass.declaredMethods.forEach { method ->
                    if (method.name == "setFieldAccessor") {
                        method.isAccessible = true

                        method.invoke(field, customAccess, true)
                    }
                }
                if (Modifier.isStatic(field.modifiers)) {
                    field.set(null, newValue)
                } else {
                    throw IllegalStateException("It is not a top property : $propertyName")
                }
                return
            }
        }

        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }
    throw IllegalArgumentException("Can't find the property named :$propertyName in the same file with ${otherCallableReference.name}")
}

/**
 * get the property value from a class object ,no matter whether the property is public ,private or intenel
 */
fun getClassPropertyValueByName(classObj: Any, propertyName: String): Any? {
    val containerClass: Class<*> = classObj::class.java

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredFields.forEach { field ->
            if (field.name == propertyName) {
                field.isAccessible = true

                return field.get(classObj)
            }
        }
        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }
    return null
}

/**
 * get the property value in the top of a kotlin file(not in any kotlin class) ,no matter whether the property is public ,private or internal
 */
fun getTopPropertyValueByName(otherCallableReference: CallableReference, propertyName: String): Any? {

    val owner = otherCallableReference.owner
    val containerClass: Class<*>
    try {
        containerClass =
            (owner!!::class.members as ArrayList).firstOrNull { it.name == "jClass" }?.call(owner) as Class<*>
    } catch (e: Exception) {
        throw IllegalArgumentException("No such property 'jClass'")
    }

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredFields.forEach { field ->

            if (field.name == propertyName) {
                field.isAccessible = true

                /**
                 * top property(package property) should be static in java level
                 * or throw an exception
                 * */
                if (Modifier.isStatic(field.modifiers)) {
                    return field.get(null)
                } else {
                    throw IllegalStateException("It is not a top property : $propertyName")
                }
            }
        }

        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }

    throw IllegalArgumentException("Can't find the property named :$propertyName in the same file with ${otherCallableReference.name}")
}

/**
 * invoke a method by name from a classObj,no matter whether the property is public ,private or internal
 */
fun invokeClassMethodByMethodName(classObj: Any, methodName: String, vararg methodArgs: Any?): Any? {
    val containerClass: Class<*> = classObj::class.java

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredMethods.forEach { method ->
            if (method.name == methodName && method.parameterTypes.size == methodArgs.size) {
                method.isAccessible = true
                removeFinalModifies(method)

                try {
                    return if (methodArgs.isNotEmpty()) {
                        method.invoke(classObj, *methodArgs)
                    } else {
                        method.invoke(classObj)
                    }
                } catch (e: IllegalArgumentException) {
                    return@forEach
                }
            }
        }

        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }

    throw IllegalArgumentException("Can't find the method named :$methodName with args ${methodArgs.toList()} in the classObj : $classObj")
}

/**
 * invoke a static method by name from a kclass,no matter whether the property is public ,private or internal
 */
fun invokeStaticClassMethodByMethodName(kClass: KClass<*>, methodName: String, vararg methodArgs: Any?): Any? {
    val containerClass: Class<*> = kClass.java

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredMethods.forEach { method ->
            if (method.name == methodName
                && method.parameterTypes.size == methodArgs.size
                && Modifier.isStatic(method.modifiers)
            ) {
                method.isAccessible = true

                removeFinalModifies(method)

                try {
                    return if (methodArgs.isNotEmpty()) {
                        method.invoke(null, *methodArgs)
                    } else {
                        method.invoke(null)
                    }
                } catch (e: IllegalArgumentException) {
                    return@forEach
                }
            }
        }

        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }

    throw IllegalArgumentException("Can't find the method named :$methodName with args ${methodArgs.toList()} in the classObj : $kClass")
}

/**
 * invoke a method by name from a kotlin file(not in any kotlin class),no matter whether the property is public ,private or internal
 */
fun invokeTopMethodByMethodName(
    otherCallableReference: CallableReference,
    methodName: String,
    vararg methodArgs: Any?
): Any? {
    val owner = otherCallableReference.owner
    val containerClass: Class<*>
    try {
        containerClass =
            (owner!!::class.members as ArrayList).firstOrNull { it.name == "jClass" }?.call(owner) as Class<*>
    } catch (e: Exception) {
        throw IllegalArgumentException("No such property 'jClass'")
    }

    var tobeSearchMethodClass: Class<*>? = containerClass

    while (tobeSearchMethodClass != null) {

        tobeSearchMethodClass.declaredMethods.forEach { method ->
            if (method.name == methodName && method.parameterTypes.size == methodArgs.size) {
                method.isAccessible = true
                removeFinalModifies(method)

                try {
                    return if (methodArgs.isNotEmpty()) {
                        method.invoke(null, *methodArgs)
                    } else {
                        method.invoke(null)
                    }
                } catch (e: IllegalArgumentException) {
                    return@forEach
                }
            }
        }
        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }
    throw IllegalArgumentException(
        "Can't find the method named :$methodName with args ${
            methodArgs.toList()
                .toString()
        } in the same file with ${otherCallableReference.name}"
    )

}

fun collectDeclaredMethod(kClass: KClass<*>, methodHandle: (Method) -> Unit) {
    collectDeclaredMethod(kClass.java, methodHandle)
}

fun collectDeclaredMethod(cls: Class<*>, methodHandle: (Method) -> Unit) {
    var tobeSearchMethodClass: Class<*>? = cls
    while (tobeSearchMethodClass != null) {
        tobeSearchMethodClass.declaredMethods.forEach(methodHandle)
        tobeSearchMethodClass = tobeSearchMethodClass.superclass
    }
}


private fun removeFinalModifies(fieldOrMethod: Member) {
    updateModifies(fieldOrMethod) {
        it and Modifier.FINAL.inv()
    }
}


private fun updateModifies(fieldOrMethod: Member, modifier: (Int) -> Int) {
    if (Modifier.isFinal(fieldOrMethod.modifiers)) {
        try {
            val modifyFiled = fieldOrMethod.javaClass.getDeclaredField("modifiers")
            modifyFiled.isAccessible = true
            modifyFiled.setInt(fieldOrMethod, modifier(modifyFiled.getInt(fieldOrMethod)))
        } catch (e: Exception) {
            Log.log("failed update modifies of $fieldOrMethod")
        }
    }
}


fun <T : AccessibleObject, R> T.privileged(handle: (T) -> R): R {
    return try {
        handle(this)
    } catch (e: IllegalAccessException) {
        this.isAccessible = true
        handle(this)
    }
}