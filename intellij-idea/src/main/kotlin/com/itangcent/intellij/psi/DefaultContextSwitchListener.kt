package com.itangcent.intellij.psi

import com.google.inject.Singleton
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.itangcent.common.utils.safeComputeIfAbsent

@Singleton
open class DefaultContextSwitchListener : ContextSwitchListener {

    private var context: PsiElement? = null

    private var module: Module? = null

    protected var moduleCache: MutableMap<String, Module> = LinkedHashMap()

    override fun switchTo(psiElement: PsiElement) {
        if (context == psiElement) {
            return
        }

        context = psiElement

        val containingFile = psiElement.containingFile ?: return
        val path = containingFile.virtualFile?.path ?: return

        val currModule = moduleCache.safeComputeIfAbsent(path) {
            ModuleUtil.findModuleForPsiElement(psiElement)
        }

        if (currModule != null && module != currModule) {
            module = currModule
            moduleChangeEvent?.invoke(currModule)
        }
    }

    override fun clear() {
        this.context = null
        this.module = null
    }

    protected var moduleChangeEvent: ((Module) -> Unit)? = null

    override fun onModuleChange(event: (Module) -> Unit) {
        moduleChangeEvent = when (moduleChangeEvent) {
            null -> event
            else -> {
                val preEvent = moduleChangeEvent!!
                {
                    preEvent(it)
                    event(it)
                }
            }
        }
    }

    protected fun setContext(context: PsiElement?) {
        this.context = context
    }

    override fun getContext(): PsiElement? {
        return context
    }

    protected fun setModule(module: Module?) {
        this.module = module
    }

    override fun getModule(): Module? {
        return module
    }
}