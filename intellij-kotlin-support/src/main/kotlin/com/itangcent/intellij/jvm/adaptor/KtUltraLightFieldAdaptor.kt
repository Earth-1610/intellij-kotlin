package com.itangcent.intellij.jvm.adaptor

import java.lang.reflect.Method
import kotlin.reflect.full.superclasses

object KtUltraLightFieldAdaptor {

    private const val ktUltraLightFieldImplClassName = "org.jetbrains.kotlin.asJava.classes.KtUltraLightFieldImpl"

    private val computeConstantValueMethod: Method by lazy {
        Class.forName(ktUltraLightFieldImplClassName)
            .methods
            .first { it.name == "computeConstantValue" }
    }

    fun computeConstantValue(any: Any): Any? {
        return try {
            computeConstantValueMethod.invoke(any)
        } catch (e: Exception) {
            null
        }
    }

    fun isKtUltraLightField(any: Any): Boolean {
        val cls = any::class
        if (cls.qualifiedName == ktUltraLightFieldImplClassName) {
            return true
        }
        for (superclass in cls.superclasses) {
            if (superclass.qualifiedName == ktUltraLightFieldImplClassName) {
                return true
            }
        }
        return false
    }
}