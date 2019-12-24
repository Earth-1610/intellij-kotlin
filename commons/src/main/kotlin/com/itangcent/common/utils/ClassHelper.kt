package com.itangcent.common.utils

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

object ClassHelper {

    private val EMPTY_CLASS_ARRAY = arrayOfNulls<Class<*>>(0)

    fun isArrayOrCollection(bean: Any?): Boolean {
        return bean != null && (bean is Collection<*> || bean is Array<*>)
    }

    fun isArray(bean: Any?): Boolean {
        return bean != null && bean is Array<*>
    }

    fun isCollection(bean: Any?): Boolean {
        return bean != null && bean is Collection<*>
    }

    fun <T : Any> newInstance(type: KClass<T>, vararg params: Any): Any {
        val constructors = type.constructors
        if (constructors.size == 1) {
            try {
                return constructors.first().call(*params)
            } catch (e: Exception) {
                throw IllegalArgumentException("Illegal params to instance Class[" + type.simpleName + "]")
            }
        }

        val constructor: KFunction<Any>?

        constructor = when {
            params.isEmpty() -> constructors.firstOrNull { it.parameters.isEmpty() }
            else -> constructors.firstOrNull {
                isAssignable(
                    params,
                    it.parameters.map { param -> param.type.classifier as KClass<*> }.toTypedArray()
                )
            }
        }

        if (constructor != null) {
            try {
                return constructor.call(*params)
            } catch (e: Exception) {
                throw IllegalArgumentException("Illegal params to instance Class[" + type.simpleName + "]")
            }
        }

        constructors.filter { it.parameters.size == params.size }.forEach {
            try {
                return it.call(*params)
            } catch (e: Exception) {
            }
        }

        throw IllegalArgumentException("Illegal params to instance Class[" + type.simpleName + "]")

    }

    fun isAssignable(params: Array<*>, paramTypes: Array<KClass<*>>): Boolean {
        if (params.size != paramTypes.size)
            return false
        for (i in 0..params.size) {

            if (!isAssignable(params[i], paramTypes[i])) {
                return false
            }
        }
        return true
    }

    fun isAssignable(params: Any?, paramType: KClass<*>): Boolean {
        return when (params) {
            null -> false
            else -> (paramType.isInstance(params))
        }
    }

    internal fun Class<*>.getDeclaredFieldInHierarchy(name: String): Field? {
        val inheritanceChain = generateSequence(this) { it.superclass }
        return inheritanceChain.map {
            try {
                it.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                null
            }
        }.filterNotNull().first()
    }

    internal inline fun <T> checkedReflection(block: () -> T, onReflectionException: (Exception) -> T): T {
        return try {
            block()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        } catch (e: ReflectiveOperationException) {
            onReflectionException(e)
        } catch (e: IllegalArgumentException) {
            onReflectionException(e)
        }
    }
}
