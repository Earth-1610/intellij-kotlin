package com.itangcent.intellij.config

import com.google.inject.Inject
import com.itangcent.common.utils.asBool
import com.itangcent.intellij.jvm.dev.AbstractDevEnv


class ConfigReaderDevEnvSupporter : AbstractDevEnv() {

    @Inject
    private val configReader: ConfigReader? = null

    override fun isDev(): Boolean {
        return configReader?.first("dev").asBool() == true
    }
}