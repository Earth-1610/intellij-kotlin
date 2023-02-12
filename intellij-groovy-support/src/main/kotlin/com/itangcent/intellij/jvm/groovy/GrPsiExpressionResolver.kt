package com.itangcent.intellij.jvm.groovy

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.itangcent.common.logger.Log
import com.itangcent.common.utils.flatten
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.PsiExpressionResolver
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyReference
import kotlin.reflect.KClass

class GrPsiExpressionResolver : PsiExpressionResolver {

    companion object : Log()

    private val psiExpressionResolver: PsiExpressionResolver = ActionContext.local()

    override fun process(psiElement: PsiElement): Any? {
        if (!GrPsiUtils.isGrPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        return doProcess(psiElement).also {
            LOG.debug(
                "process grElement: type:${psiElement::class}, text:【${psiElement.text.flatten()}】" +
                        "-> $it"
            )
        }
    }

    override fun process(psiExpression: PsiExpression): Any? {
        if (!GrPsiUtils.isGrPsiInst(psiExpression)) {
            throw NotImplementedError()
        }

        return doProcess(psiExpression).also {
            LOG.debug(
                "process grElement: type:${psiExpression::class}, text:【${psiExpression.text.flatten()}】" +
                        "-> $it"
            )
        }
    }

    private fun doProcess(psiElement: Any): Any? {

        when {
            psiElement is GroovyReference -> {
                return psiElement.resolve(true).firstOrNull()?.element?.let {
                    psiExpressionResolver.process(it)
                }
            }
        }
        throw NotImplementedError("not implemented")
    }

    override fun processStaticField(psiField: PsiField): Any? {
        TODO("Not yet implemented")
    }

    override fun <T : Any> registerExpressionResolver(cls: KClass<T>, handle: (T) -> Any?) {
        TODO("Not yet implemented")
    }

    override fun <T : Any> registerExpressionResolver(predicate: (Any) -> Boolean, handle: (T) -> Any?) {
        TODO("Not yet implemented")
    }

}