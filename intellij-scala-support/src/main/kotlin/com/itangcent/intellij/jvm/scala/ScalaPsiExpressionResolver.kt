package com.itangcent.intellij.jvm.scala

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.PsiExpressionResolver
import com.itangcent.intellij.jvm.scala.compatible.ScCompatibleLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import java.util.*
import kotlin.reflect.KClass

class ScalaPsiExpressionResolver : PsiExpressionResolver {

    private val psiExpressionResolver: PsiExpressionResolver = ActionContext.local()

    override fun process(psiElement: PsiElement): Any? {
        if (!ScPsiUtils.isScPsiInst(psiElement)) {
            throw NotImplementedError()
        }
        if (ScCompatibleLiteral.isInstance(psiElement)) {
            return ScCompatibleLiteral.getValue(psiElement) ?: ScCompatibleLiteral.getText(psiElement)
        }

        if (psiElement is ScMethodCall) {
            if (psiElement.invokedExpr.text == "Array") {
                val list = LinkedList<Any>()
                for (argumentExpression in psiElement.argumentExpressions()) {
                    psiExpressionResolver.process(argumentExpression)
                        ?.let { list.add(it) }
                }
                return list
            }
        }

        throw NotImplementedError("not implemented")
    }

    override fun process(psiExpression: PsiExpression): Any? {
        TODO("Not yet implemented")
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