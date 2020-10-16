package com.itangcent.intellij.jvm.adaptor

import com.itangcent.common.utils.invokeMethod
import kotlin.reflect.full.superclasses

object KtUltraLightFieldAdaptor {

    fun computeConstantValue(any: Any): Any? {
        return try {
            any.invokeMethod("computeConstantValue")
        } catch (e: Exception) {
            null
        }
    }

    fun isKtUltraLightField(any: Any): Boolean {
        val cls = any::class
        if (cls.qualifiedName == "org.jetbrains.kotlin.asJava.classes.KtUltraLightFieldImpl") {
            return true
        }
        for (superclass in cls.superclasses) {
            if (superclass.qualifiedName == "org.jetbrains.kotlin.asJava.classes.KtUltraLightFieldImpl") {
                return true
            }
        }
        return false
    }

}