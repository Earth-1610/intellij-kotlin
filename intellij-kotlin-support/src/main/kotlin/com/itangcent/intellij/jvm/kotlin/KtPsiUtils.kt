package com.itangcent.intellij.jvm.kotlin

object KtPsiUtils {

    /**
     * bean which class qualifiedName contains kotlin may be a instance
     * of class that in kotlin-plugin
     * e.g.
     * [org.jetbrains.kotlin]
     * [org.jetbrains.kotlin.psi]
     */
    fun isKtPsiInst(any: Any): Boolean {
        return any::class.qualifiedName?.contains("kotlin") ?: false
    }
}