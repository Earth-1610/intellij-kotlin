package com.itangcent.common.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

class RegisterExclusionStrategy : ExclusionStrategy {

    var exclusiveFields: ArrayList<FieldAttributes> = ArrayList()
    var exclusiveClass: ArrayList<Class<*>> = ArrayList()

    fun exclude(aClass: Class<*>): RegisterExclusionStrategy {
        exclusiveClass.add(aClass)
        return this
    }

    fun exclude(field: FieldAttributes): RegisterExclusionStrategy {
        exclusiveFields.add(field)
        return this
    }

    override fun shouldSkipField(fieldAttributes: FieldAttributes): Boolean {
        return exclusiveFields.contains(fieldAttributes)
    }

    override fun shouldSkipClass(aClass: Class<*>): Boolean {
        if (exclusiveClass.contains(aClass)) {
            return true
        }
        return exclusiveClass.any { it.isAssignableFrom(aClass) }
    }
}
