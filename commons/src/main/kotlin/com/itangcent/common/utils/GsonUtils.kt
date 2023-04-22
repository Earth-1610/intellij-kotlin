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
            val numberObjectTypeAdapter = NumberFixedObjectTypeAdapter()
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
                .disableHtmlEscaping()
                .setLenient()
                .create()
                .fix()
                .also {
                    numberObjectTypeAdapter.setGson(it)
                }
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [gson].", e)
            Gson()
        }
    }

    private val gsonWithNulls: Gson by lazy {
        try {
            val numberObjectTypeAdapter = NumberFixedObjectTypeAdapter()
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
                .serializeNulls()
                .disableHtmlEscaping()
                .setLenient()
                .create()
                .fix()
                .also {
                    numberObjectTypeAdapter.setGson(it)
                }
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [gsonWithNulls].", e)
            Gson()
        }
    }

    private val prettyGson: Gson by lazy {
        try {
            val numberObjectTypeAdapter = NumberFixedObjectTypeAdapter()
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .setLenient()
                .create()
                .fix()
                .also {
                    numberObjectTypeAdapter.setGson(it)
                }
        } catch (e: Exception) {
            LOG.traceError("failed init GSON module [prettyGson].", e)
            Gson()
        }
    }

    private val pretty_gson_with_nulls: Gson by lazy {
        try {
            val numberObjectTypeAdapter = NumberFixedObjectTypeAdapter()
            GsonBuilder()
                .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
                .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
                .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
                .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .setLenient()
                .create()
                .fix()
                .also {
                    numberObjectTypeAdapter.setGson(it)
                }
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
        return pretty_gson_with_nulls.toJson(any)
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
 * By default ObjectTypeAdapter,all number are deserialized to double.
 * It's not necessary to keep long to long,float to float.
 * But shouldn't deserialize short/int/long to double
 * So fix it.
 */
class NumberFixedObjectTypeAdapter : TypeAdapter<Any> {

    private var gson: Gson? = null

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

        val typeAdapter = gson!!.getAdapter(value.javaClass) as TypeAdapter<Any>
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


/**
 * try to remove default ObjectTypeAdapter from gson
 * if success,the {@link NumberFixedObjectTypeAdapter} will be used instead
 */
@Suppress("UNCHECKED_CAST")
private fun Gson.fix(): Gson {
    var factories = this.getPropertyValue("factories") ?: return this

    //unwrap for UnmodifiableList
    if (factories::class.qualifiedName == "java.util.Collections.UnmodifiableRandomAccessList" ||
        factories::class.qualifiedName == "java.util.Collections.UnmodifiableList"
    ) {
        factories = factories.getPropertyValue("list") ?: return this
    }
    val objectTypeAdapterClassName = ObjectTypeAdapter::class.qualifiedName!!
    (factories as MutableList<TypeAdapterFactory>).removeIf {
        it::class.java.name.startsWith(objectTypeAdapterClassName)
    }
    return this
}
