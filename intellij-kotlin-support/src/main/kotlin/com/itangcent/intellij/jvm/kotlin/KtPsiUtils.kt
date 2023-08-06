package com.itangcent.intellij.jvm.kotlin

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.psi.KtClass

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

    fun ktClassToPsiClass(ktClass: KtClass): PsiClass {
        return KtLightClassForSourceDeclaration.create(ktClass, JvmDefaultMode.ALL_COMPATIBILITY)!!
    }
}