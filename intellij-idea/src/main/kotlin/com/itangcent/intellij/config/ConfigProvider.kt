package com.itangcent.intellij.config

interface ConfigProvider {
    fun loadConfig(): Sequence<ConfigContent>
}