package com.itangcent.intellij.jvm.adaptor

import java.lang.reflect.Method
import kotlin.reflect.full.superclasses

object KtLightFieldAdaptor {

    private const val ktFieldImplClassName = "org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl"

    private val computeConstantValueMethod: Method by lazy {
        Class.forName(ktFieldImplClassName)
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

    fun isKtLightField(any: Any): Boolean {
        val cls = any::class
        if (cls.qualifiedName == ktFieldImplClassName) {
            return true
        }
        for (superclass in cls.superclasses) {
            if (superclass.qualifiedName == ktFieldImplClassName) {
                return true
            }
        }
        return false
    }
}