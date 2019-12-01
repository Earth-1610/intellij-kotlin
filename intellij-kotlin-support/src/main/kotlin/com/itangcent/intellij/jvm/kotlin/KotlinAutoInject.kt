package com.itangcent.intellij.jvm.kotlin

import com.itangcent.common.SetupAble
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.withUnsafe
import com.itangcent.intellij.jvm.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class KotlinAutoInject : SetupAble {

    override fun init() {
        try {
            val classLoader = KotlinAutoInject::class.java.classLoader
            if (classLoader.loadClass("org.jetbrains.kotlin.psi.KtClass") != null) {

                tryLoadAndBind(
                    classLoader, DocHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinDocHelper"
                )
                tryLoadAndBind(
                    classLoader,
                    AnnotationHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinAnnotationHelper"
                )
                tryLoadAndBind(
                    classLoader,
                    JvmClassHelper::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinJvmClassHelper"
                )
                tryLoadAndBind(
                    classLoader,
                    LinkExtractor::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinLinkExtractor"
                )
                tryLoadAndBind(
                    classLoader,
                    PsiResolver::class,
                    "com.itangcent.intellij.jvm.kotlin.KotlinPsiResolver"
                )
            }
        } catch (e: Exception) {
        }
    }

    private fun tryLoadAndBind(
        classLoader: ClassLoader,
        injectClass: KClass<*>,
        bindClassName: String
    ) {
        try {
            val bindClass =
                classLoader.loadClass(bindClassName).kotlin

            ActionContext.addDefaultInject { actionContextBuilder ->
                actionContextBuilder.bind(injectClass) {
                    it.withUnsafe(bindClass)
                }
            }
        } catch (e: Exception) {
        }
    }
}