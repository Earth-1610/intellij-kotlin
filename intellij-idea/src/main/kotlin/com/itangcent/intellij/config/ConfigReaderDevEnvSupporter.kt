package com.itangcent.intellij.config

import com.google.inject.Inject
import com.google.inject.Singleton
import com.itangcent.common.utils.asBool
import com.itangcent.intellij.jvm.dev.AbstractDevEnv


@Singleton
class ConfigReaderDevEnvSupporter : AbstractDevEnv() {

    @Inject
    private val configReader: ConfigReader? = null

    override fun isDev(): Boolean {
        return configReader?.first("dev").asBool() == true
    }
}