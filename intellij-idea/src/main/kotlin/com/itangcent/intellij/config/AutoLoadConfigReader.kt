package com.itangcent.intellij.config

import com.itangcent.intellij.extend.guice.PostConstruct

abstract class AutoLoadConfigReader : BaseConfigReader() {

    protected abstract val configProviders: MutableList<ConfigProvider>

    @PostConstruct
    fun loadConfig() {
        configProviders.flatMap { it.loadConfig() }
            .forEach { this.loadConfigInfoContent(it.content, it.type) }
    }

    override fun reset() {
        super.reset()
        loadConfig()
    }
}
