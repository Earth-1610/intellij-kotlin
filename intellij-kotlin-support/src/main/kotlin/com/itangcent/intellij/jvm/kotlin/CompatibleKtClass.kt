package com.itangcent.intellij.jvm.kotlin

import kotlin.reflect.KClass

object CompatibleKtClass {

    private var ktLightPsiLiteralClass: KClass<*>? = null

    init {
        val classLoader = CompatibleKtClass::class.java.classLoader
        try {
            ktLightPsiLiteralClass =
                    classLoader.loadClass("org.jetbrains.kotlin.asJava.elements.KtLightPsiLiteral").kotlin
        } catch (e: Exception) {
        }
    }

    fun isKtLightPsiLiteral(any: Any): Boolean {
        return ktLightPsiLiteralClass?.isInstance(any) ?: false
    }
}