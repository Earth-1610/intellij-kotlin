package com.itangcent.intellij.jvm.kotlin

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SetupAble
import com.itangcent.common.spi.SpiUtils
import com.itangcent.intellij.jvm.*
import com.itangcent.intellij.jvm.spi.AutoInjectKit


@Suppress("UNCHECKED_CAST")
class KotlinAutoInject : SetupAble {

    override fun init() {
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)

        try {
            logger?.debug("try load kotlin injects")
            val classLoader = KotlinAutoInject::class.java.classLoader
            if (AutoInjectKit.tryLoad(classLoader, "org.jetbrains.kotlin.psi.KtClass") != null) {
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    DocHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinDocHelper"
                )

                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    AnnotationHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinAnnotationHelper"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    JvmClassHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinJvmClassHelper"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    LinkExtractor::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinLinkExtractor"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    PsiResolver::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinPsiResolver"
                )
            }
        } catch (e: Throwable) {
            logger?.traceError("load kotlin injects failed", e)
        }
    }
}