package com.itangcent.intellij.jvm.kotlin

import com.itangcent.common.SetupAble
import com.itangcent.intellij.jvm.*

@Suppress("UNCHECKED_CAST")
class KotlinAutoInject : SetupAble {

    override fun init() {
        try {
            val classLoader = KotlinAutoInject::class.java.classLoader
            if (classLoader.loadClass("org.jetbrains.kotlin.psi.KtClass") != null) {

                AutoInjectKit.tryLoadAndBind(
                    classLoader, DocHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinDocHelper"
                )
                AutoInjectKit.tryLoadAndBind(
                    classLoader,
                    AnnotationHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinAnnotationHelper"
                )
                AutoInjectKit.tryLoadAndWrap(
                    classLoader,
                    JvmClassHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinJvmClassHelper"
                )
                AutoInjectKit.tryLoadAndBind(
                    classLoader,
                    LinkExtractor::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinLinkExtractor"
                )
                AutoInjectKit.tryLoadAndBind(
                    classLoader,
                    PsiResolver::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinPsiResolver"
                )
            }
        } catch (e: Exception) {
        }
    }

}