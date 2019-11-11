package com.itangcent.intellij.psi

import com.google.inject.ImplementedBy
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement

@ImplementedBy(DefaultContextSwitchListener::class)
interface ContextSwitchListener {

    fun switchTo(psiElement: PsiElement)

    fun clear()

    fun onModuleChange(event: (Module) -> Unit)

    fun getContext(): PsiElement?

    fun getModule(): Module?
}