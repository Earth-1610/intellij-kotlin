/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itangcent.common.utils

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.*

/**
 * Adapts types whose static type is only 'Object'. Uses getClass() on
 * serialization and a primitive/Map/List on deserialization.
 */
class NumberObjectTypeAdapter internal constructor(private val gson: Gson) : TypeAdapter<Any>() {

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Any? {
        val token = `in`.peek()
        when (token) {
            JsonToken.BEGIN_ARRAY -> {
                val list = ArrayList<Any?>()
                `in`.beginArray()
                while (`in`.hasNext()) {
                    list.add(read(`in`))
                }
                `in`.endArray()
                return list
            }

            JsonToken.BEGIN_OBJECT -> {
                val map = LinkedTreeMap<String, Any>()
                `in`.beginObject()
                while (`in`.hasNext()) {
                    map[`in`.nextName()] = read(`in`)
                }
                `in`.endObject()
                return map
            }

            JsonToken.STRING -> return `in`.nextString()

            JsonToken.NUMBER -> {
                val numberStr = `in`.nextString()

                if (numberStr.contains(".") || numberStr.contains("e")
                    || numberStr.contains("E")
                ) {
                    return numberStr.toDouble()
                }

                return Integer.parseInt(numberStr)
            }

            JsonToken.BOOLEAN -> return `in`.nextBoolean()

            JsonToken.NULL -> {
                `in`.nextNull()
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
        if (typeAdapter is NumberObjectTypeAdapter) {
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
                    NumberObjectTypeAdapter(gson) as TypeAdapter<T>
                } else null
            }
        }
    }
}
