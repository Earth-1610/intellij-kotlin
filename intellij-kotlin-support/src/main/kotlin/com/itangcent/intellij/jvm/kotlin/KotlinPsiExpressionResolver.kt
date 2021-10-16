package com.itangcent.intellij.jvm.kotlin

import com.google.inject.Inject
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.tree.IElementType
import com.itangcent.common.utils.flatten
import com.itangcent.common.utils.invokeMethod
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.jvm.PsiExpressionResolver
import com.itangcent.intellij.jvm.PsiResolver
import com.itangcent.intellij.jvm.adaptor.KtUltraLightFieldAdaptor
import com.itangcent.intellij.jvm.dev.DevEnv
import com.itangcent.intellij.jvm.standard.Operand
import com.itangcent.intellij.jvm.standard.StandardOperand
import com.itangcent.intellij.logger.Logger
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import kotlin.reflect.KClass

class KotlinPsiExpressionResolver : PsiExpressionResolver {

    private val psiExpressionResolver: PsiExpressionResolver = ActionContext.local()

    private val psiResolver: PsiResolver = ActionContext.local()

    override fun process(psiElement: PsiElement): Any? {
        if (!KtPsiUtils.isKtPsiInst(psiElement)) {
            throw NotImplementedError()
        }

        LOG.debug("process ktElement: type:${psiElement::class}, text:【${psiElement.text.flatten()}】")

        when {
            CompatibleKtClass.isKtLightPsiLiteral(psiElement) -> {
                val value = psiElement.invokeMethod("getValue")
                if (value != null) {
                    if (value is String || value::class.java.isPrimitive) {
                        return value
                    }
                }
                return psiElement.text
            }
            psiElement is KtBinaryExpression -> {
                return processBinaryExpression(psiElement)
            }
            psiElement is KtLightFieldImpl<*> -> {
                return psiElement.computeConstantValue()
            }
            KtUltraLightFieldAdaptor.isKtUltraLightField(psiElement) -> {
                return KtUltraLightFieldAdaptor.computeConstantValue(psiElement)
            }
            psiElement is KtCollectionLiteralExpression -> {
                return psiElement.getInnerExpressions().map { psiExpressionResolver.process(it) }.toTypedArray()
            }
            psiElement is KtDotQualifiedExpression -> {
                return processDot(psiElement)
            }
            psiElement is KtNameReferenceExpression -> {
                return psiElement.getIdentifier()?.let { psiExpressionResolver.process(it) }
            }
            psiElement is KtStringTemplateExpression -> {
                return psiElement.plainContent
            }
            psiElement is KtCallExpression -> {
                return psiElement.valueArgumentList?.let { psiExpressionResolver.process(it) }
            }
            psiElement is KtValueArgumentList -> {
                val map = LinkedHashMap<Any?, Any?>()
                for (argument in psiElement.arguments) {
                    if (argument.isNamed()) {
                        argument.getArgumentName()?.let { psiExpressionResolver.process(it) }
                            ?.let { name ->
                                argument.getArgumentExpression()?.let {
                                    map[name] = psiExpressionResolver.process(it)
                                }
                            }
                    } else {
                        argument.getArgumentExpression()?.let { map["value"] = psiExpressionResolver.process(it) }
                    }
                }
                return map
            }
            psiElement is KtValueArgument -> {
                return psiElement.getArgumentExpression()?.let { psiExpressionResolver.process(it) }
            }
            psiElement is KtValueArgumentName -> {
                val name = psiElement.asName
                if (name.isSpecial) {
                    return name.identifier
                }
                return psiElement.text
            }

        }
        throw NotImplementedError("not implemented")
    }

    private fun processDot(ktDotQualifiedExpression: KtDotQualifiedExpression): Any? {
        val dotText = ktDotQualifiedExpression.text
        var classWithPropertyOrMethod = psiResolver.resolveClassWithPropertyOrMethod(
            dotText,
            ktDotQualifiedExpression
        )
        if (classWithPropertyOrMethod?.second == null) {
            if (!dotText.contains('#')) {
                val index = dotText.lastIndexOf('.')
                if (index == -1) {
                    return null
                }
                classWithPropertyOrMethod = psiResolver.resolveClassWithPropertyOrMethod(
                    dotText.substring(0, index) + "#" + dotText.substring(index + 1),
                    ktDotQualifiedExpression
                )
            }
        }
        return classWithPropertyOrMethod?.second?.let { process(it) }
    }

    private fun processBinaryExpression(psiExpression: KtBinaryExpression): Any? {
        val op = psiExpression.operationToken
        val operand = KotlinOperand.findOperand(op) ?: return null
        val lOperand = psiExpression.left?.let { psiExpressionResolver.process(it) }
        val rOperand = psiExpression.right?.let { psiExpressionResolver.process(it) }
        return operand.operate(lOperand, rOperand)
    }

    override fun process(psiExpression: PsiExpression): Any? {

        if (!KtPsiUtils.isKtPsiInst(psiExpression)) {
            throw NotImplementedError()
        }

        LOG.debug("process ktPsiExpression: type:${psiExpression::class}, text:【${psiExpression.text.flatten()}】")
        when {
            CompatibleKtClass.isKtLightPsiLiteral(psiExpression) -> {
                val value = psiExpression.invokeMethod("getValue")
                if (value != null) {
                    if (value is String || value::class.java.isPrimitive) {
                        return value
                    }
                }
                return psiExpression.text
            }
        }

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

    enum class KotlinOperand : Operand {
        ;

        companion object {
            fun findOperand(op: IElementType): Operand? {
                return when (op) {
                    KtTokens.PLUS -> {
                        StandardOperand.PLUS
                    }
                    KtTokens.MINUS -> {
                        StandardOperand.MINUS
                    }
                    KtTokens.MUL -> {
                        StandardOperand.ASTERISK
                    }
                    KtTokens.DIV -> {
                        StandardOperand.DIV
                    }
                    KtTokens.EQ -> {
                        StandardOperand.EQ
                    }
                    KtTokens.EQEQ -> {
                        StandardOperand.EQEQ
                    }
                    KtTokens.EXCLEQ -> {
                        StandardOperand.NE
                    }
                    KtTokens.GT -> {
                        StandardOperand.GT
                    }
                    KtTokens.GTEQ -> {
                        StandardOperand.GE
                    }
                    KtTokens.LT -> {
                        StandardOperand.LT
                    }
                    KtTokens.LTEQ -> {
                        StandardOperand.LE
                    }
                    KtTokens.PERC -> {
                        StandardOperand.PERC
                    }
                    KtTokens.ANDAND -> {
                        StandardOperand.ANDAND
                    }
                    KtTokens.OROR -> {
                        StandardOperand.OROR
                    }
                    else -> StandardOperand.findOperand(op)
                }
            }
        }
    }
}


//background idea log
private val LOG = org.apache.log4j.Logger.getLogger(KotlinPsiExpressionResolver::class.java)