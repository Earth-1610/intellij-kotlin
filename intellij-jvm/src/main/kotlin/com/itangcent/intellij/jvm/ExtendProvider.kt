package com.itangcent.intellij.jvm

import com.intellij.psi.PsiField

interface ExtendProvider {

    fun extraDoc(field: PsiField): String?
}