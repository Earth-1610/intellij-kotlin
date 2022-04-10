package com.itangcent.intellij.util

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.intellij.context.ActionContext
import org.apache.commons.lang.StringUtils
import java.io.File

/**
 * Created by tangcent on 2017/2/16.
 */
object ActionUtils {

    fun findCurrentPath(): String? {
        val actionContext = ActionContext.getContext()!!
        val psiFile = actionContext.cacheOrCompute(CommonDataKeys.PSI_FILE.name) {
            actionContext.callInReadUI { actionContext.instance(DataContext::class).getData(CommonDataKeys.PSI_FILE) }
        }
        if (psiFile != null) return findCurrentPath(psiFile)

        val navigatable = actionContext.cacheOrCompute(CommonDataKeys.NAVIGATABLE.name) {
            actionContext.callInReadUI {
                actionContext.instance(DataContext::class).getData(CommonDataKeys.NAVIGATABLE)
            }
        }
        if (navigatable != null && navigatable is PsiDirectory) {//select dir
            return findCurrentPath(navigatable)
        }
        val navigatables = actionContext.cacheOrCompute(CommonDataKeys.NAVIGATABLE_ARRAY.name) {
            actionContext.callInReadUI {
                actionContext.instance(DataContext::class).getData(CommonDataKeys.NAVIGATABLE_ARRAY)
            }
        }
        if (navigatables != null) {//select mult dir
            for (node in navigatables) {
                when (navigatable) {
                    is PsiDirectory -> {//select dir
                        return findCurrentPath(navigatable)
                    }
                    is ClassTreeNode -> {
                        return findCurrentPath(navigatable.psiClass.containingFile)
                    }
                    is PsiDirectoryNode -> {
                        return navigatable.element?.value?.let { findCurrentPath(it) }
                    }
                }
            }
        }

        val project = ActionContext.getContext()!!.instance(Project::class)
        return project.basePath
    }

    fun findCurrentPath(psiFile: PsiFile): String? {
        val dir = ActionContext.getContext()!!.callInReadUI { psiFile.parent }
        return dir?.let { findCurrentPath(it) } + File.separator + psiFile.name
    }

    fun findCurrentPath(psiDirectory: PsiDirectory): String? {
        var dirPath = psiDirectory.toString()
        if (dirPath.contains(':')) {
            dirPath = StringUtils.substringAfter(dirPath, ":")
        }
        return dirPath
    }

    fun findCurrentClass(): PsiClass? {
        val actionContext = ActionContext.getContext()!!
        val editor = actionContext.cacheOrCompute(CommonDataKeys.EDITOR.name) {
            actionContext.callInReadUI { actionContext.instance(DataContext::class).getData(CommonDataKeys.EDITOR) }
        } ?: return null
        val psiFile = actionContext.cacheOrCompute(CommonDataKeys.PSI_FILE.name) {
            actionContext.callInReadUI { actionContext.instance(DataContext::class).getData(CommonDataKeys.PSI_FILE) }
        } ?: return null
        var referenceAt = psiFile.findElementAt(editor.caretModel.offset)
        var cls: PsiClass? = null
        try {
            cls = PsiTreeUtil.getContextOfType<PsiElement>(referenceAt, PsiClass::class.java) as PsiClass?
        } catch (e: Exception) {
            //ignore
        }
        if (cls == null) {
            val document = editor.document
            referenceAt = psiFile.findElementAt(DocumentUtils.getInsertIndex(document))
            try {
                cls = PsiTreeUtil.getContextOfType<PsiElement>(referenceAt, PsiClass::class.java) as PsiClass?
            } catch (e: Exception) {
            }
        }
        return cls
    }

    fun format(anActionEvent: AnActionEvent) {
        doAction(anActionEvent, "ReformatCode")
    }

    fun optimize(anActionEvent: AnActionEvent) {
        doAction(anActionEvent, "OptimizeImports")
    }

    fun doAction(anActionEvent: AnActionEvent, action: String) {
        try {
            anActionEvent.actionManager.getAction(action).actionPerformed(anActionEvent)
        } catch (e: Exception) {
            LOG.warn("failed doAction:$action", e)
        }

    }

}

//background idea log
private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ActionUtils::class.java)