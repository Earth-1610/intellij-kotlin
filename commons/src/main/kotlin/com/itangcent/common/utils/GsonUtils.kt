package com.itangcent.common.utils

import com.google.gson.*
import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.ArrayList
import kotlin.reflect.KClass

/**
 *
 * @author tangcent
 */
object GsonUtils {

    private val numberObjectTypeAdapter = NumberFixedObjectTypeAdapter()

    private val gson = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
        .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
        .create()

    private val gsonWithNulls = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
        .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
        .serializeNulls()
        .create()

    private val prettyGson = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
        .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
        .setPrettyPrinting()
        .create()

    private val pretty_gson_with_nulls = GsonBuilder()
        .setExclusionStrategies(RegisterExclusionStrategy().exclude(Visional::class.java))
        .registerTypeAdapter(Any::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(Map::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(List::class.java, numberObjectTypeAdapter)
        .registerTypeAdapter(LazilyParsedNumber::class.java, LazilyParsedNumberTypeAdapter())
        .registerTypeAdapterFactory(NumberFixedObjectTypeAdapter.FACTORY)
        .setPrettyPrinting()
        .serializeNulls()
        .create()

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
        return JsonParser().parse(str)
    }

    fun <T> fromJson(json: String, cls: Class<T>): T {
        return gson.fromJson(json, cls)
    }

    fun <T : Any> fromJson(json: String, cls: KClass<T>): T {
        return gson.fromJson(json, cls.java)
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

    @Deprecated(message = "use com.itangcent.common.utils.AnyKitKt.resolveCycle",
        replaceWith = ReplaceWith("com.itangcent.common.utils.resolveCycle")
    )
    fun resolveCycle(any: Any?): Any? = any.resolveCycle()

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
        val token = reader.peek()
        when (token) {
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
