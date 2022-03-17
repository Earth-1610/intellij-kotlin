package com.itangcent.intellij.jvm

import com.itangcent.common.utils.safeComputeIfAbsent
import kotlin.reflect.KClass

object ClassMateDataStorage {

    /**
     * className -> mateData
     */
    private val mateData = HashMap<String, ClassMateData>()

    /**
     * tag -> classes
     */
    private val classWithTagCache = HashMap<String, Array<String>>()

    fun classWithTag(tag: String): Array<String> {
        return classWithTagCache.safeComputeIfAbsent(tag) {
            mateData.entries.filter { it.value.tags?.contains(tag) == true }
                .map { it.key }
                .toTypedArray()
        } ?: emptyArray()
    }

    fun hasTag(className: String, tag: String): Boolean {
        return mateData[className]?.tags?.contains(tag) ?: false
    }

    fun addTag(className: String, vararg tags: String) {
        mateDataOf(className).addTags(*tags)
    }

    fun addTag(cls: Class<*>, vararg tags: String) {
        addTag(cls.name, *tags)
        addTag(cls.simpleName, *tags)
    }

    fun addTag(cls: KClass<*>, vararg tags: String) {
        cls.qualifiedName?.let { addTag(it, *tags) }
        cls.simpleName?.let { addTag(it, *tags) }
    }

    fun setDefaultValue(className: String, defaultValue: Any) {
        mateDataOf(className).defaultValue = defaultValue
    }

    fun setDefaultValue(cls: Class<*>, defaultValue: Any) {
        setDefaultValue(cls.name, defaultValue)
        setDefaultValue(cls.simpleName, defaultValue)
    }

    fun setDefaultValue(cls: KClass<*>, defaultValue: Any) {
        cls.qualifiedName?.let { setDefaultValue(it, defaultValue) }
        cls.simpleName?.let { setDefaultValue(it, defaultValue) }
    }

    fun getDefaultValue(className: String): Any? {
        return mateData[className]?.defaultValue
    }

    private fun mateDataOf(className: String): ClassMateData {
        return mateData.safeComputeIfAbsent(className) {
            ClassMateData()
        }!!
    }

    class ClassMateData {

        var tags: Array<String>? = null

        var defaultValue: Any? = null

        @Synchronized
        fun addTags(vararg tags: String) {
            if (this.tags == null) {
                this.tags = arrayOf(*tags)
            } else {
                this.tags = this.tags!! + tags
            }
        }
    }
}
