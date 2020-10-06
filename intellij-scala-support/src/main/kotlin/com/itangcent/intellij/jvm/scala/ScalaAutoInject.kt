package com.itangcent.intellij.jvm.scala

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.spi.AutoInjectKit

@Suppress("UNCHECKED_CAST")
class ScalaAutoInject : SetupAble {

    override fun init() {
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)
        try {
            logger?.debug("try load scala injects")
            val classLoader = ScalaAutoInject::class.java.classLoader
            if (AutoInjectKit.tryLoad(
                    classLoader,
                    "org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression"
                ) != null
            ) {
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    JvmClassHelper::class,
                    "com.itangcent.intellij.jvm.scala.ScalaJvmClassHelper"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    DocHelper::class,
                    "com.itangcent.intellij.jvm.scala.ScalaDocHelper"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    AnnotationHelper::class,
                    "com.itangcent.intellij.jvm.scala.ScalaAnnotationHelper"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    LinkExtractor::class,
                    "com.itangcent.intellij.jvm.scala.ScalaLinkExtractor"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    PsiResolver::class,
                    "com.itangcent.intellij.jvm.scala.ScalaPsiResolver"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    PsiExpressionResolver::class,
                    "com.itangcent.intellij.jvm.scala.ScalaPsiExpressionResolver"
                )
            }
        } catch (e: Exception) {
            logger?.traceError("load scala injects failed", e)
        }
    }

}