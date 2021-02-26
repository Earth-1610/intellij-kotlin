package com.itangcent.common.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
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

    fun resolveCycle(any: Any?): Any? = any.resolveCycle()

    fun copy(any: Any?): Any? = any.copy()


}
