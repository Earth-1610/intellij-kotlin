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

    fun hasAcceptedType(className: String, acceptedType: String): Boolean {
        return mateData[className]?.acceptedTypes?.contains(acceptedType) ?: false
    }

    fun addAcceptedType(className: String, vararg acceptedTypes: String) {
        mateDataOf(className).addAcceptedTypes(*acceptedTypes)
    }

    fun addAcceptedType(cls: Class<*>, vararg acceptedTypes: String) {
        addAcceptedType(cls.name, *acceptedTypes)
        addAcceptedType(cls.simpleName, *acceptedTypes)
    }

    fun addAcceptedType(cls: KClass<*>, vararg acceptedTypes: String) {
        cls.qualifiedName?.let { addAcceptedType(it, *acceptedTypes) }
        cls.simpleName?.let { addAcceptedType(it, *acceptedTypes) }
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

    fun isAccepted(oneClass: String, anotherClass: String):Boolean {
        mateDataOf(oneClass).acceptedTypes?.contains(anotherClass)?.takeIf { it }?.let { return true}
        mateDataOf(anotherClass).acceptedTypes?.contains(oneClass)?.takeIf { it }?.let { return true}
        return false
    }

    class ClassMateData {

        var tags: Array<String>? = null

        var defaultValue: Any? = null

        var acceptedTypes:Array<String>? = null

        @Synchronized
        fun addTags(vararg tags: String) {
            if (this.tags == null) {
                this.tags = arrayOf(*tags)
            } else {
                this.tags = this.tags!! + tags
            }
        }
        
        @Synchronized
        fun addAcceptedTypes(vararg types: String) {
            if (this.acceptedTypes == null) {
                this.acceptedTypes = arrayOf(*types)
            } else {
                this.acceptedTypes = this.acceptedTypes!! + types
            }
        }
    }
}
