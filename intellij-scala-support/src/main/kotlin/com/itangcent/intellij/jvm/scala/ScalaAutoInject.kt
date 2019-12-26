package com.itangcent.intellij.jvm.scala

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.spi.AutoInjectKit
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.JvmClassHelper

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
            }
        } catch (e: Exception) {
            logger?.traceError("load scala injects failed", e)
        }
    }

}