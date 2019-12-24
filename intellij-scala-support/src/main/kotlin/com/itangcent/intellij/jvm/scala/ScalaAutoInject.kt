package com.itangcent.intellij.jvm.scala

import com.itangcent.common.SetupAble
import com.itangcent.intellij.jvm.AutoInjectKit
import com.itangcent.intellij.jvm.DocHelper
import com.itangcent.intellij.jvm.JvmClassHelper

@Suppress("UNCHECKED_CAST")
class ScalaAutoInject : SetupAble {

    override fun init() {
        try {
            val classLoader = ScalaAutoInject::class.java.classLoader
            if (classLoader.loadClass("org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass") != null) {
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
            }
        } catch (e: Exception) {
        }
    }

}