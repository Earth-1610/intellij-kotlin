package com.itangcent.intellij.jvm.kotlin

import com.itangcent.common.SetupAble
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.with
import com.itangcent.intellij.jvm.AnnotationHelper
import com.itangcent.intellij.jvm.DocHelper
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class KotlinAutoInject : SetupAble {

    override fun init() {
        try {
            val classLoader = KotlinAutoInject::class.java.classLoader
            if (classLoader.loadClass("org.jetbrains.kotlin.psi.KtClass") != null) {
                val kotlinDocHelperClass =
                    classLoader.loadClass("com.itangcent.intellij.jvm.kotlin.KotlinDocHelper").kotlin
                val kotlinAnnotationHelperClass =
                    classLoader.loadClass("com.itangcent.intellij.jvm.kotlin.KotlinAnnotationHelper").kotlin

                ActionContext.addDefaultInject { actionContextBuilder ->
                    actionContextBuilder.bind(DocHelper::class) { it.with(kotlinDocHelperClass as KClass<DocHelper>) }
                    actionContextBuilder.bind(AnnotationHelper::class) { it.with(kotlinAnnotationHelperClass as KClass<AnnotationHelper>) }
                }
            }
        } catch (e: Exception) {
        }
    }
}