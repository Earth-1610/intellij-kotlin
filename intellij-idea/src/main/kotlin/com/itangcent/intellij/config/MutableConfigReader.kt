package com.itangcent.intellij.config

interface MutableConfigReader : ConfigReader {
    fun put(key: String, vararg value: String)

    fun remove(key: String)

    fun remove(key: String, value: String)

    fun loadConfigInfoContent(configInfoContent: String)
}
