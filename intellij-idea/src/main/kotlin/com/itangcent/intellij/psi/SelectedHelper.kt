package com.itangcent.intellij.psi

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.PostConstruct
import com.itangcent.intellij.util.DirFilter
import com.itangcent.intellij.util.FileFilter
import com.itangcent.intellij.util.FileUtils

object SelectedHelper {

    class Builder {

        private var fileHandle: ((PsiFile) -> Unit)? = null
        private var dirFilter: DirFilter? = null
        private var classHandle: ((PsiClass) -> Unit)? = null
        private var fileFilter: FileFilter = { true }

        private var contextSwitchListener: ContextSwitchListener? = ActionContext.getContext()
            ?.instance(ContextSwitchListener::class)

        fun fileHandle(fileHandle: (PsiFile) -> Unit): Builder {
            this.fileHandle = fileHandle
            return this
        }

        fun fileFilter(fileFilter: FileFilter): Builder {
            this.fileFilter = fileFilter
            return this
        }

        fun dirFilter(dirFilter: DirFilter): Builder {
            this.dirFilter = dirFilter
            return this
        }

        fun classHandle(classHandle: (PsiClass) -> Unit): Builder {
            this.classHandle = classHandle
            return this
        }

        private val actionContext = ActionContext.getContext()!!

        @Suppress("UNCHECKED_CAST")
        fun traversal() {
            val (type, target) = actionContext.instance(SelectedContext::class).selected() ?: return
            when (type) {
                CommonDataKeys.NAVIGATABLE_ARRAY -> {
                    onNavigatables(target as Array<Navigatable>)
                }

                CommonDataKeys.PSI_FILE -> {
                    actionContext.runInReadUI {
                        onFile(target as PsiFile)
                    }
                }

                CommonDataKeys.NAVIGATABLE -> {
                    actionContext.runInReadUI {
                        onNavigatable(target as Navigatable)
                    }
                }
            }
        }

        private fun onNavigatables(navigatables: Array<Navigatable>?) {
            if (navigatables != null && navigatables.isNotEmpty()) {
                val boundary = actionContext.createBoundary()
                try {
                    for (navigatable in navigatables) {
                        actionContext.runInReadUI {
                            onNavigatable(navigatable)
                        }
                        boundary.waitComplete(false)
                        Thread.sleep(100)
                    }
                } finally {
                    boundary.remove()
                }
            }
        }

        private fun onNavigatable(navigatable: Navigatable) {
            when (navigatable) {
                is PsiDirectory -> {//select dir
                    onDirectory(navigatable)
                }

                is PsiClass -> {//select class
                    onClass(navigatable)
                }

                is ClassTreeNode -> {
                    onClass(navigatable.psiClass)
                }

                is PsiDirectoryNode -> {
                    navigatable.element?.value?.let { onDirectory(it) }
                }

                is PsiMember -> {
                    navigatable.containingClass?.let { onClass(it) }
                }
            }
        }

        private fun onFile(psiFile: PsiFile) {

            contextSwitchListener?.switchTo(psiFile)
            if (fileHandle != null) fileHandle!!(psiFile)
            if (classHandle != null && psiFile is PsiClassOwner) {
                for (psiCls in psiFile.classes) {
                    classHandle!!(psiCls)
                }
            }
        }

        private fun onClass(psiClass: PsiClass) {
            if (classHandle != null) classHandle!!(psiClass)
            if (fileHandle != null) fileHandle!!(psiClass.containingFile)
        }

        private fun onDirectory(psiDirectory: PsiDirectory) {
            if (dirFilter == null) {
                actionContext.runInReadUI {
                    contextSwitchListener?.switchTo(psiDirectory)
                    FileUtils.traversal(psiDirectory, fileFilter) {
                        onFile(it)
                    }
                }
            } else {
                dirFilter!!(psiDirectory) {
                    if (it) {
                        actionContext.runInReadUI {
                            contextSwitchListener?.switchTo(psiDirectory)
                            FileUtils.traversal(psiDirectory, fileFilter) { file ->
                                onFile(file)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Singleton
class SelectedContext {

    @Inject(optional = true)
    private var dataContextProvider: DataContextProvider? = null

    private var context: Pair<DataKey<out Any>, Any>? = null

    @PostConstruct
    fun init() {
        findContext()
    }

    @Synchronized
    fun findContext() {
        if (context != null || dataContextProvider == null) {
            return
        }

        //try to get navigatables
        val navigatables = dataContextProvider?.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
        if (navigatables != null && navigatables.size > 1) {
            context = CommonDataKeys.NAVIGATABLE_ARRAY to navigatables
            return
        }

        //try to get psiFile
        val psiFile = dataContextProvider?.getData(CommonDataKeys.PSI_FILE)
        if (psiFile != null) {
            context = CommonDataKeys.PSI_FILE to psiFile
            return
        }

        //try to process navigatables
        if (!navigatables.isNullOrEmpty()) {
            context = CommonDataKeys.NAVIGATABLE_ARRAY to navigatables
            return
        }

        //try to get navigatable
        val navigatable = dataContextProvider?.getData(CommonDataKeys.NAVIGATABLE)
        if (navigatable != null) {
            context = CommonDataKeys.NAVIGATABLE to navigatable
            return
        }
    }

    fun selected(): Pair<DataKey<out Any>, Any>? {
        if (context == null) {
            findContext()
        }
        return context
    }
}