package com.itangcent.intellij.jvm.feature

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.spi.AutoInjectKit


class FeatureAutoInject : SetupAble {

    override fun init() {
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)

        try {
            logger?.debug("try load java feature injects")
            val classLoader = FeatureAutoInject::class.java.classLoader
            if (AutoInjectKit.tryLoad(classLoader, "com.intellij.psi.impl.light.LightRecordField") != null) {
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    DocHelper::class,
                    "com.itangcent.intellij.jvm.feature.RecordDocHelper"
                )
            }
        } catch (e: Throwable) {
            logger?.traceError("load java feature injects failed", e)
        }
    }
}