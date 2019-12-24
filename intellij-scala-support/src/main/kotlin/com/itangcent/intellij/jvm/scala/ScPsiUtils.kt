package com.itangcent.intellij.jvm.scala

object ScPsiUtils {

    /**
     * bean which class qualifiedName contains scala may be a instance
     * of class that in scala-plugin
     * e.g.
     * [org.jetbrains.plugins.scala]
     */
    fun isScPsiInst(any: Any): Boolean {
        return any::class.qualifiedName?.contains("scala") ?: false
    }
}