package com.itangcent.intellij.config

interface ConfigReader {

    fun readConfigInfo(): Map<String, String>

    fun read(key: String): String?

    fun foreach(action: (String, String) -> Unit)

    fun foreach(keyFilter: (String) -> Boolean, action: (String, String) -> Unit)
}
