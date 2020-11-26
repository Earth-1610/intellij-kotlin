package com.itangcent.common.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.util.*
import kotlin.reflect.KClass

/**
 *
 * @author tangcent
 */
object GsonUtils {
    private val gson = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapterFactory(NumberObjectTypeAdapter.FACTORY)
        .create()

    private val gsonWithNulls = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapterFactory(NumberObjectTypeAdapter.FACTORY)
        .serializeNulls()
        .create()

    private val prettyGson = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapterFactory(NumberObjectTypeAdapter.FACTORY)
        .setPrettyPrinting()
        .create()

    private val pretty_gson_with_nulls = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapterFactory(NumberObjectTypeAdapter.FACTORY)
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    fun toJsonSafely(any: Any?): String {
        return gsonWithNulls.toJson(resolveCycle(any))
    }

    fun toJson(any: Any?): String {
        return gson.toJson(any)
    }

    fun toJsonWithNulls(any: Any?): String {
        return gsonWithNulls.toJson(resolveCycle(any))
    }

    fun parseToJsonTree(str: String?): JsonElement? {
        return JsonParser().parse(str)
    }

    fun <T> fromJson(json: String, cls: Class<T>): T {
        return gson.fromJson(json, cls)
    }

    fun <T : Any> fromJson(json: String, cls: KClass<T>): T {
        return gson.fromJson(json, cls.java)
    }

    fun prettyJsonSafely(any: Any?): String {
        return prettyGson.toJson(resolveCycle(any))
    }

    fun prettyJson(any: Any?): String {
        return prettyGson.toJson(any)
    }

    fun prettyJsonWithNulls(any: Any?): String {
        return pretty_gson_with_nulls.toJson(any)
    }

    fun resolveCycle(any: Any?): Any? {
        when (any) {
            null -> return null
            is Map<*, *>, is Collection<*> -> {
                if (!collectValues(any)) {
                    return copy(any)
                }
                return any
            }
            else -> return any
        }
    }

    fun copy(any: Any?): Any? {
        return when (any) {
            null -> null
            is Map<*, *> -> copy(any, ValuePresence())
            is Collection<*> -> copy(any, ValuePresence())
            else -> any
        }
    }

    private fun copy(any: Any?, values: ValuePresence): Any? {

        when (any) {
            null -> return null
            is Map<*, *> -> {
                if (!values.add(any)) return emptyMap<Any?, Any?>()
                val newCopy: HashMap<Any?, Any?> = HashMap()
                any.forEach { (key, value) ->
                    newCopy[key] = copy(value, values)
                }
                values.pop()
                return newCopy
            }
            is Collection<*> -> {
                if (!values.add(any)) return ArrayList<Any?>()
                val newCopy: ArrayList<Any?> = ArrayList()
                any.forEach { value ->
                    copy(value, values)?.let { newCopy.add(it) }
                }
                values.pop()
                return newCopy
            }
            else -> return any
        }
    }

    private fun collectValues(any: Any?): Boolean {
        return when (any) {
            null -> true
            is Map<*, *> -> collectValues(any, ValuePresence())
            is Collection<*> -> collectValues(any, ValuePresence())
            else -> true
        }
    }

    private fun collectValues(any: Any?, values: ValuePresence): Boolean {

        if (any == null) return true

        if (any is Map<*, *>) {
            if (!values.add(any)) return false

            for (value in any.values) {
                if (!collectValues(value, values))
                    return false
            }
            values.pop()

            return true
        }

        if (any is Collection<*>) {
            if (!values.add(any)) return false
            for (value in any) {
                if (!collectValues(value, values))
                    return false
            }

            values.pop()
        }
        return true
    }

    private class ValuePresence {

        private val values: Stack<Any> = Stack()

        fun add(one: Any?): Boolean {
            if (one == null) return true
            values.filter { it === one }
                .any { return false }
            values.add(one)
            return true
        }

        fun pop() {
            values.pop()
        }

        fun clear() {
            values.clear()
        }
    }

}
