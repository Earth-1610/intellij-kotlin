package com.itangcent.common.string

import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.GsonUtils

interface GracefulToString {
    fun toString(any: Any?): String?
}

fun Any?.gracefulString(): String? {
    val services = SpiUtils.loadServices(GracefulToString::class) ?: return this?.toString()
    for (service in services) {
        service.toString(this)?.let { return it }
    }
    if(this==null){
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