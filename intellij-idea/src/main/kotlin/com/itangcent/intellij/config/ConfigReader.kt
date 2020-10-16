package com.itangcent.intellij.config

import com.itangcent.common.utils.asBool

interface ConfigReader {

    fun first(key: String): String?

    fun read(key: String): Collection<String>?

    fun foreach(action: (String, String) -> Unit)

    fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit)

    fun resolveProperty(property: String): String
}


fun ConfigReader.dev(action: () -> Unit) {
    if (this.first("dev").asBool() == true) {
        action()
    }
}