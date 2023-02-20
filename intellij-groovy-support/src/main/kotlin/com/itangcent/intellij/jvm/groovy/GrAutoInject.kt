package com.itangcent.intellij.jvm.groovy

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.jvm.PsiExpressionResolver
import com.itangcent.intellij.jvm.spi.AutoInjectKit


class GrAutoInject : SetupAble {

    override fun init() {
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)

        try {
            logger?.debug("try load kotlin injects")
            val classLoader = GrAutoInject::class.java.classLoader
            AutoInjectKit.tryLoadAndWrap(
                classLoader,
                PsiExpressionResolver::class,
                "com.itangcent.intellij.jvm.groovy.GrPsiExpressionResolver"
            )
        } catch (e: Throwable) {
            logger?.traceError("load kotlin injects failed", e)
        }
    }
}