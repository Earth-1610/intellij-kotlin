package com.itangcent.intellij.util

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.common.utils.cast
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

    fun findCurrentPath(psiFile: PsiFile): String {
        val dir = ActionContext.getContext()!!.callInReadUI { psiFile.parent }
        return dir?.let { findCurrentPath(it) } + File.separator + psiFile.name
    }

    fun findCurrentPath(psiDirectory: PsiDirectory): String {
        var dirPath = psiDirectory.toString()
        if (dirPath.contains(':')) {
            dirPath = StringUtils.substringAfter(dirPath, ":")
        }
        return dirPath
    }

    fun findCurrentClass(): PsiClass? {
        findContextOfType<PsiClass>()?.let { return it }
        val actionContext = ActionContext.getContext()!!
        return actionContext.cacheOrCompute(CommonDataKeys.PSI_FILE.name) {
            actionContext.callInReadUI { actionContext.instance(DataContext::class).getData(CommonDataKeys.PSI_FILE) }
        }.cast(PsiClassOwner::class)?.classes?.firstOrNull()
    }

    fun findCurrentMethod(): PsiMethod? {
        val actionContext = ActionContext.getContext()!!
        actionContext.cacheOrCompute(CommonDataKeys.PSI_ELEMENT.name) {
            actionContext.callInReadUI {
                actionContext.instance(DataContext::class).getData(CommonDataKeys.PSI_ELEMENT)
            }
        }?.let { actionContext.callInReadUI {PsiTreeUtil.getContextOfType<PsiElement>(it, PsiMethod::class.java)} }
            .cast(PsiMethod::class)
            ?.let { return it }

        return findContextOfType()
    }

    inline fun <reified T : PsiElement> findContextOfType(): T? {
        return findContextOfType(T::class.java)
    }

    fun <T : PsiElement> findContextOfType(cls: Class<T>): T? {
        val actionContext = ActionContext.getContext()!!
        val editor = actionContext.cacheOrCompute(CommonDataKeys.EDITOR.name) {
            actionContext.callInReadUI { actionContext.instance(DataContext::class).getData(CommonDataKeys.EDITOR) }
        } ?: return null
        val psiFile = actionContext.cacheOrCompute(CommonDataKeys.PSI_FILE.name) {
            actionContext.callInReadUI { actionContext.instance(DataContext::class).getData(CommonDataKeys.PSI_FILE) }
        } ?: return null
        val referenceAt = actionContext.callInReadUI { psiFile.findElementAt(editor.caretModel.offset) } ?: return null
        try {
            return actionContext.callInReadUI { PsiTreeUtil.getContextOfType(referenceAt, cls) }
        } catch (e: Exception) {
            //ignore
        }
        return null

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