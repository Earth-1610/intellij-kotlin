package com.itangcent.intellij.psi

import com.google.inject.Singleton
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.itangcent.common.utils.safeComputeIfAbsent

@Singleton
open class DefaultContextSwitchListener : ContextSwitchListener {

    private var context: PsiElement? = null

    @Volatile
    private var module: Module? = null

    protected var moduleCache: MutableMap<String, Module> = LinkedHashMap()

    override fun switchTo(psiElement: PsiElement) {
        if (context == psiElement) {
            return
        }

        context = psiElement

        val containingFile = when (psiElement) {
            is PsiFile -> psiElement
            else -> psiElement.containingFile
        } ?: return
        val path = containingFile.virtualFile?.path ?: return

        val nextModule = moduleCache.safeComputeIfAbsent(path) {
            ModuleUtil.findModuleForPsiElement(psiElement)
        }

        if (nextModule != null && module != nextModule) {
            synchronized(this) {
                if (module != nextModule) {
                    module = nextModule
                    moduleChangeEvent?.invoke(nextModule)
                }
            }
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
        //call if any module is already selected
        module?.let(event)
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