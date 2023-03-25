package com.itangcent.intellij.jvm.groovy

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.PsiExpressionResolver
import com.itangcent.intellij.jvm.spi.AutoInjectKit


class GrAutoInject : SetupAble {

    override fun init() {
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)

        try {
            logger?.debug("try load groovy injects")
            val classLoader = GrAutoInject::class.java.classLoader
            if (AutoInjectKit.tryLoad(
                    classLoader,
                    "org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField"
                ) != null
            ) {
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    PsiExpressionResolver::class,
                    "com.itangcent.intellij.jvm.groovy.GrPsiExpressionResolver"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    DocHelper::class,
                    "com.itangcent.intellij.jvm.groovy.GrDocHelper"
                )
            }
        } catch (e: Throwable) {
            logger?.traceError("load kotlin groovy failed", e)
        }
    }
}