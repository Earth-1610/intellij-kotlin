package com.itangcent.intellij.psi

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.itangcent.common.concurrent.AQSCountLatch
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.logger.Logger
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

object SelectedHelper {

    class Builder {

        private var fileHandle: ((PsiFile) -> Unit)? = null
        private var dirFilter: DirFilter? = null
        private var classHandle: ((PsiClass) -> Unit)? = null
        private var onCompleted: (() -> Unit)? = null
        private var timeOut: Long? = null
        private var fileFilter: FileFilter = { true }

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

        fun onCompleted(onCompleted: () -> Unit): Builder {
            this.onCompleted = onCompleted
            return this
        }

        private val aqsCount = AQSCountLatch()

        private val actionContext = ActionContext.getContext()!!
        private val logger = actionContext.instance(Logger::class)

        private var semaphoreForDirHandle = Semaphore(1)

        public fun traversal() {
            aqsCount.down()

            actionContext.runAsync {
                if (timeOut == null) {
                    aqsCount.waitForUnsafe()
                } else {
                    aqsCount.waitForUnsafe(TimeUnit.MINUTES.toMillis(timeOut!!))
                }
                onCompleted?.invoke()
            }
            actionContext.runInReadUI {
                try {
                    val psiFile = actionContext.cacheOrCompute(CommonDataKeys.PSI_FILE.name) {
                        actionContext.instance(AnActionEvent::class).getData(CommonDataKeys.PSI_FILE)
                    }
                    if (psiFile != null) {
                        onFile(psiFile)
                        return@runInReadUI
                    }
                } catch (e: Exception) {
                    logger.warn("error handle class")
                }

                try {
                    val navigatable = actionContext.cacheOrCompute(CommonDataKeys.NAVIGATABLE.name) {
                        actionContext.instance(AnActionEvent::class).getData(CommonDataKeys.NAVIGATABLE)
                    }
                    if (navigatable != null) {
                        onNavigatable(navigatable)
                        return@runInReadUI
                    }
                } catch (e: Exception) {
                    logger.warn("error handle navigatable")
                }

                try {
                    val navigatables = actionContext.cacheOrCompute(CommonDataKeys.NAVIGATABLE_ARRAY.name) {
                        actionContext.instance(AnActionEvent::class).getData(CommonDataKeys.NAVIGATABLE_ARRAY)
                    }
                    if (navigatables != null && navigatables.isNotEmpty()) {
                        try {
                            for (navigatable in navigatables) {
                                aqsCount.down()
                                onNavigatable(navigatable)
                            }
                        } finally {
                            aqsCount.up()
                        }
                        return@runInReadUI
                    }
                } catch (e: Exception) {
                    logger.warn("error handle navigatables")
                }

                aqsCount.up()
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
                else -> aqsCount.up()
            }
        }

        private fun onFile(psiFile: PsiFile) {
            try {
                if (fileHandle != null) fileHandle!!(psiFile)
                if (classHandle != null && psiFile is PsiClassOwner) {
                    actionContext.runInReadUI {
                        for (psiCls in psiFile.classes) {
                            classHandle!!(psiCls)
                        }
                    }
                }
            } finally {
                aqsCount.up()
            }
        }

        private fun onClass(psiClass: PsiClass) {
            try {
                if (classHandle != null) classHandle!!(psiClass)
                if (fileHandle != null) fileHandle!!(psiClass.containingFile)
            } finally {
                aqsCount.up()
            }
        }

        private fun onDirectory(psiDirectory: PsiDirectory) {
            if (dirFilter == null) {
                try {
                    actionContext.runInReadUI {
                        SelectedHelper.traversal(psiDirectory, fileFilter, {
                            aqsCount.down()
                            onFile(it)
                        })
                    }
                } finally {
                    aqsCount.up()
                }
            } else {
                dirFilter!!(psiDirectory) {
                    if (it) {
                        actionContext.runInReadUI {
                            try {
                                SelectedHelper.traversal(psiDirectory, fileFilter, { file ->
                                    aqsCount.down()
                                    onFile(file)
                                })
                            } finally {
                                aqsCount.up()
                            }
                        }
                    } else {
                        aqsCount.up()
                    }
                }
            }
        }
    }

    fun traversal(
        psiDirectory: PsiDirectory,
        fileFilter: FileFilter,
        fileHandle: (PsiFile) -> Unit
    ) {

        val dirStack: Stack<PsiDirectory> = Stack()
        var dir: PsiDirectory? = psiDirectory
        while (dir != null) {
            dir.files.filter { fileFilter(it) }
                .forEach { fileHandle(it) }

            for (subdirectory in dir.subdirectories) {
                dirStack.push(subdirectory)
            }
            if (dirStack.isEmpty()) break
            dir = dirStack.pop()
        }
    }
}

typealias FileFilter = (PsiFile) -> Boolean

typealias DirFilter = (PsiDirectory, (Boolean) -> Unit) -> Unit