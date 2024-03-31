package com.itangcent.intellij.config

import com.google.inject.ImplementedBy
import com.itangcent.common.utils.asBool

@ImplementedBy(EmptyConfigReader::class)
interface ConfigReader {

    fun first(key: String): String?

    fun read(key: String): Collection<String>?

    fun foreach(action: (String, String) -> Unit)

    fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit)

    fun resolveProperty(property: String): String
}

class EmptyConfigReader : ConfigReader {
    override fun first(key: String): String? {
        return null
    }

    override fun read(key: String): Collection<String>? {
        return null
    }

    override fun foreach(action: (String, String) -> Unit) {
    }

    override fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit) {
    }

    override fun resolveProperty(property: String): String {
        return property
    }
}

fun ConfigReader.dev(action: () -> Unit) {
    if (this.first("dev").asBool() == true) {
        action()
    }
}