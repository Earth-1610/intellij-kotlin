package com.itangcent.intellij.util

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.cast
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.PsiResolver
import com.itangcent.intellij.psi.DataContextProvider
import com.itangcent.intellij.psi.SelectedContext
import org.apache.commons.lang.StringUtils
import java.io.File
import kotlin.reflect.KClass

/**
 * Created by tangcent on 2017/2/16.
 */
object ActionUtils : Log() {

    @Suppress("UNCHECKED_CAST")
    fun findCurrentPath(): String? {
        val actionContext = ActionContext.getContext()!!
        val selectedContext = actionContext.instance(SelectedContext::class).selected()
        if (selectedContext != null) {
            val (type, target) = selectedContext
            when (type) {
                CommonDataKeys.PSI_FILE -> {
                    return findCurrentPath(target as PsiFile)
                }

                CommonDataKeys.NAVIGATABLE -> {
                    //select dir
                    if (target != null && target is PsiDirectory) {
                        return findCurrentPath(target)
                    }
                }

                CommonDataKeys.NAVIGATABLE_ARRAY -> {
                    val navigatables = target as Array<Navigatable>
                    for (navigatable in navigatables) {
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
        return actionContext.instance(DataContextProvider::class).getData(CommonDataKeys.PSI_FILE)
            .cast(PsiClassOwner::class)?.classes?.firstOrNull()
    }

    fun findCurrentMethod(): PsiMethod? {
        val actionContext = ActionContext.getContext()!!
        actionContext.instance(DataContextProvider::class).getData(CommonDataKeys.PSI_ELEMENT)
            ?.let { actionContext.callInReadUI { PsiTreeUtil.getContextOfType<PsiElement>(it, PsiMethod::class.java) } }
            .cast(PsiMethod::class)
            ?.let { return it }

        return findContextOfType()
    }

    inline fun <reified T : PsiElement> findContextOfType(): T? {
        return findContextOfType(T::class)
    }

    fun <T : PsiElement> findContextOfType(cls: KClass<T>): T? {
        val actionContext = ActionContext.getContext()!!
        val editor = actionContext.instance(DataContextProvider::class).getData(CommonDataKeys.EDITOR) ?: return null
        val psiFile = actionContext.instance(DataContextProvider::class).getData(CommonDataKeys.PSI_FILE) ?: return null
        val referenceAt = actionContext.callInReadUI { psiFile.findElementAt(editor.caretModel.offset) } ?: return null
        try {
            return actionContext.callInReadUI {
                actionContext.instance(PsiResolver::class).getContextOfType(referenceAt, cls)
            }
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
            LOG.traceWarn("failed doAction:$action", e)
        }

    }
}