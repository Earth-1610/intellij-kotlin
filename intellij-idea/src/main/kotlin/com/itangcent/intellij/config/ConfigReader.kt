package com.itangcent.intellij.config

interface ConfigReader {

    fun first(key: String): String?

    fun read(key: String): Collection<String>?

    fun foreach(action: (String, String) -> Unit)

    fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit)
}
