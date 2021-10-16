package com.itangcent.common.string

import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.GsonUtils
import javax.script.ScriptEngine

interface GracefulToString {
    fun toString(any: Any?): String?
}

private val GRACEFUL_TO_STRING_INSTANCE: GracefulToString by lazy {
    SpiUtils.loadUltimateBean(GracefulToString::class)!!
}

fun Any?.gracefulString(): String? {
    GRACEFUL_TO_STRING_INSTANCE.toString(this)?.let { return it }
    if (this == null) {
        return null
    }
    return try {
        GsonUtils.toJson(this)
    } catch (e: Exception) {
        this.toString()
    }
}

class StringGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        return (any as? String)
    }
}


class ArrayGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        return (any as? Array<*>)?.joinToString(separator = ",") { it.gracefulString() ?: "" }
    }
}

class CollectionGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        return (any as? Collection<*>)?.joinToString(separator = ",") { it.gracefulString() ?: "" }
    }
}