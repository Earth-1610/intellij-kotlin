package com.itangcent.intellij.context

import com.itangcent.intellij.extend.guice.KotlinModule
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.logger.ConsoleRunnerLogger
import com.itangcent.intellij.logger.Logger

open class BasicPluginModule : KotlinModule() {
    override fun configure() {
        super.configure()
        bind(Logger::class).with(ConsoleRunnerLogger::class).singleton()
    }
}

