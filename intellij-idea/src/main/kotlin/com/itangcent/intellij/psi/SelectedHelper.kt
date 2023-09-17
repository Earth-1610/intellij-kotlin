package com.itangcent.intellij.psi

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.itangcent.common.logger.traceWarn
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
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

        private val logger = actionContext.instance(Logger::class)

        fun traversal() {

            //try to get navigatables
            try {
                val navigatables = actionContext.cacheOrCompute(CommonDataKeys.NAVIGATABLE_ARRAY.name) {
                    actionContext.instance(DataContext::class).getData(CommonDataKeys.NAVIGATABLE_ARRAY)
                }
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
                    return
                }
            } catch (e: Exception) {
                logger.traceWarn("error handle navigatables", e)
            }

            //try to get navigatable
            try {
                val navigatable = actionContext.cacheOrCompute(CommonDataKeys.NAVIGATABLE.name) {
                    actionContext.instance(DataContext::class).getData(CommonDataKeys.NAVIGATABLE)
                }
                if (navigatable != null) {
                    actionContext.runInReadUI {
                        onNavigatable(navigatable)
                    }
                    return
                }
            } catch (e: Exception) {
                logger.traceWarn("error handle navigatable", e)
            }

            //try to get psiFile
            try {
                val psiFile = actionContext.cacheOrCompute(CommonDataKeys.PSI_FILE.name) {
                    actionContext.instance(DataContext::class).getData(CommonDataKeys.PSI_FILE)
                }
                if (psiFile != null) {
                    if (fileFilter(psiFile)) {
                        actionContext.runInReadUI {
                            onFile(psiFile)
                        }
                    }
                    return
                }
            } catch (e: Exception) {
                logger.traceWarn("error handle class", e)
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