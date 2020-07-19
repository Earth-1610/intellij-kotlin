/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itangcent.common.utils

import java.util.*

/**
 * copy from {@link com.intellij.openapi.util.MultiValuesMap}
 */
class MultiValuesMap<K, V>(private val myOrdered: Boolean = false) : Map<K, Collection<V>?> {
    private val myBaseMap: MutableMap<K, Collection<V>> = if (myOrdered) LinkedHashMap() else HashMap()

    override val entries: Set<Map.Entry<K, Collection<V>?>>
        get() = myBaseMap.entries

    override val keys: Set<K>
        get() = myBaseMap.keys

    override val size: Int
        get() = myBaseMap.size

    override val values: Collection<Collection<V>?>
        get() = myBaseMap.values

    override fun containsValue(value: Collection<V>?): Boolean {
        return myBaseMap.containsValue(value)
    }

    override fun isEmpty(): Boolean {
        return myBaseMap.isEmpty()
    }

    fun putAll(key: K, values: Collection<V>) {
        for (value in values) {
            put(key, value)
        }
    }

    fun putAll(key: K, vararg values: V) {
        for (value in values) {
            put(key, value)
        }
    }

    fun put(key: K, value: V) {
        var collection: MutableCollection<V>? = myBaseMap[key] as MutableCollection<V>?
        if (collection == null) {
            collection = if (myOrdered) LinkedHashSet() else HashSet()
            myBaseMap[key] = collection
        }

        collection.add(value)
    }

    fun replace(key: K, value: V) {
        var collection: MutableCollection<V>? = myBaseMap[key] as MutableCollection<V>?
        if (collection == null) {
            collection = if (myOrdered) LinkedHashSet() else HashSet()
            myBaseMap[key] = collection
            collection.add(value)
        } else {
            collection.clear()
            collection.add(value)
        }
    }

    override operator fun get(key: K): Collection<V>? {
        return myBaseMap[key]
    }

    fun flattenValues(): Collection<V> {
        val result = if (myOrdered) LinkedHashSet() else HashSet<V>()
        for (values in myBaseMap.values) {
            result.addAll(values)
        }

        return result
    }

    fun flattenForEach(action: (K, V) -> Unit) {
        forEach { key, values ->
            values?.forEach { value ->
                action(key, value)
            }
        }
    }

    fun flattenForEach(keyFilter: (K) -> Boolean, action: (K, V) -> Unit) {
        forEach { key, values ->
            if (keyFilter(key)) {
                values?.forEach { value ->
                    action(key, value)
                }
            }
        }
    }

    fun remove(key: K, value: V) {
        if (!myBaseMap.containsKey(key)) return
        val values = myBaseMap[key] as MutableCollection<V>?
        values?.remove(value)
        if (values.isNullOrEmpty()) {
            myBaseMap.remove(key)
        }
    }

    fun clear() {
        myBaseMap.clear()
    }

    fun removeAll(key: K): Collection<V>? {
        return myBaseMap.remove(key)
    }

    override fun containsKey(key: K): Boolean {
        return myBaseMap.containsKey(key)
    }

    fun getFirst(key: K): V? {
        return myBaseMap[key]?.first()
    }

    /**
     * it will throw an IllegalArgumentException if the value of the key is not unique
     */
    fun getOne(key: K): V? {
        val valCollection = myBaseMap[key]
        return when {
            valCollection.isNullOrEmpty() -> null
            valCollection!!.size == 1 -> valCollection.first()
            else -> throw IllegalArgumentException("$key has more than one value")
        }
    }

}
