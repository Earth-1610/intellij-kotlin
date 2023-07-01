package com.itangcent.common.utils

import com.google.gson.*
import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.internal.bind.ObjectTypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceError
import java.io.IOException
import java.io.StringReader
import kotlin.reflect.KClass


/**
 *
 * @author tangcent
 */
object GsonUtils : Log() {

    private val gson: Gson by lazy {
        try {
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .disableHtmlEscaping()
                .setLenient()
                .buildWithObjectToNumberStrategy()
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [gson].", e)
            Gson()
        }
    }

    private val gsonWithNulls: Gson by lazy {
        try {
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .disableHtmlEscaping()
                .setLenient()
                .serializeNulls()
                .buildWithObjectToNumberStrategy()
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [gsonWithNulls].", e)
            Gson()
        }
    }

    private val prettyGson: Gson by lazy {
        try {
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .disableHtmlEscaping()
                .setLenient()
                .setPrettyPrinting()
                .buildWithObjectToNumberStrategy()
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [prettyGson].", e)
            Gson()
        }
    }

    private val prettyGsonWithNulls: Gson by lazy {
        try {
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .disableHtmlEscaping()
                .setLenient()
                .setPrettyPrinting()
                .serializeNulls()
                .buildWithObjectToNumberStrategy()
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [pretty_gson_with_nulls].", e)
            Gson()
        }
    }

    fun toJsonSafely(any: Any?): String {
        return gsonWithNulls.toJson(any.resolveCycle())
    }

    fun toJson(any: Any?): String {
        return gson.toJson(any)
    }

    fun toJsonWithNulls(any: Any?): String {
        return gsonWithNulls.toJson(any.resolveCycle())
    }

    fun parseToJsonTree(str: String?): JsonElement? {
        if (str == null) {
            return null
        }
        return JsonParser.parseReader(JsonReader(StringReader(str)).apply { isLenient = true })
    }

    fun <T> fromJson(json: String, cls: Class<T>): T {
        return gson.fromJson(json, cls)
    }

    fun <T : Any> fromJson(json: String, cls: KClass<T>): T {
        return gson.fromJson(json, cls.java)
    }

    inline fun <reified T> fromJson(json: String): T? {
        return fromJson(json, T::class.java)
    }

    fun prettyJsonSafely(any: Any?): String {
        return prettyGson.toJson(any.resolveCycle())
    }

    fun prettyJson(any: Any?): String {
        return prettyGson.toJson(any)
    }

    fun prettyJsonWithNulls(any: Any?): String {
        return prettyGsonWithNulls.toJson(any)
    }

    fun copy(any: Any?): Any? = any.copy()
}

/**
 * support LazilyParsedNumber
 * write as raw number instead of {"value":number}
 */
class LazilyParsedNumberTypeAdapter : TypeAdapter<LazilyParsedNumber>() {

    @Throws(IOException::class)
    override fun read(reader: JsonReader): LazilyParsedNumber? {
        return when (reader.peek()) {
            JsonToken.STRING -> LazilyParsedNumber(reader.nextString())
            JsonToken.NUMBER -> {
                LazilyParsedNumber(reader.nextString())
            }

            JsonToken.NULL -> {
                reader.nextNull()
                null
            }

            else -> throw IllegalStateException()
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: LazilyParsedNumber?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.jsonValue(value.toString())
    }
}

/**
 * By default, the ObjectTypeAdapter deserializes all numbers as Double.
 * Overrides the default behavior of number deserialization to preserve the type.
 */
class NumberFixedObjectTypeAdapter : TypeAdapter<Any> {

    private lateinit var gson: Gson

    constructor(gson: Gson) : super() {
        this.gson = gson
    }

    constructor() : super()

    fun setGson(gson: Gson) {
        this.gson = gson
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Any? {
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                val list = ArrayList<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(read(reader))
                }
                reader.endArray()
                return list
            }

            JsonToken.BEGIN_OBJECT -> {
                val map = HashMap<String, Any?>()
                reader.beginObject()
                while (reader.hasNext()) {
                    map[reader.nextName()] = read(reader)
                }
                reader.endObject()
                return map
            }

            JsonToken.STRING -> return reader.nextString()

            JsonToken.NUMBER -> {
                //read as String
                val numberStr = reader.nextString()

                // deserialized as double if ./e/E be found
                if (numberStr.contains(".") || numberStr.contains("e")
                    || numberStr.contains("E")
                ) {
                    return numberStr.toDouble()
                }

                return try {
                    numberStr.toInt()
                } catch (e: Exception) {
                    numberStr.toLong()
                }
            }

            JsonToken.BOOLEAN -> return reader.nextBoolean()

            JsonToken.NULL -> {
                reader.nextNull()
                return null
            }

            else -> throw IllegalStateException()
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Any?) {
        if (value == null) {
            out.nullValue()
            return
        }

        val typeAdapter = gson.getAdapter(value.javaClass) as TypeAdapter<Any>
        if (typeAdapter is NumberFixedObjectTypeAdapter) {
            out.beginObject()
            out.endObject()
            return
        }

        typeAdapter.write(out, value)
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        val FACTORY: TypeAdapterFactory = object : TypeAdapterFactory {
            override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                return if (type.rawType == Any::class.java) {
                    NumberFixedObjectTypeAdapter(gson) as TypeAdapter<T>
                } else null
            }
        }
    }
}

private fun GsonBuilder.buildWithObjectToNumberStrategy(): Gson {
    try {
        return this.serializeSpecialFloatingPointValues()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()
    } catch (e: Error) {
        val numberObjectTypeAdapter = NumberFixedObjectTypeAdapter()
        return this.registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
            .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
            .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
            .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
            .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
            .create()
            .apply {
                numberObjectTypeAdapter.setGson(this)
                fixFactories()
            }

    }
}

/**
 * try to remove default ObjectTypeAdapter from gson
 * if success,the {@link NumberFixedObjectTypeAdapter} will be used instead
 */
@Suppress("UNCHECKED_CAST")
private fun Gson.fixFactories(): Gson {
    try {
        val factories = this.getPropertyValue("factories") ?: return this
        factories as List<TypeAdapterFactory>
        if (factories.any { it == NumberFixedObjectTypeAdapter.FACTORY }) {
            val objectTypeAdapterClassName = ObjectTypeAdapter::class.qualifiedName!!
            val (objectTypeAdapters, others) = factories.partition {
                it::class.java.name.startsWith(objectTypeAdapterClassName)
            }
            this.changePropertyValue("factories", others + objectTypeAdapters)
        }
    } catch (e: Exception) {
        Log.warn("fix Gson failed.")
    }
    return this
}
