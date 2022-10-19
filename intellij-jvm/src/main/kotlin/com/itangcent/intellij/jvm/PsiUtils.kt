package com.itangcent.intellij.jvm

import com.intellij.lang.jvm.JvmNamedElement
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.itangcent.common.utils.cast
import com.itangcent.intellij.context.ActionContext

object PsiUtils {

    fun resolveExpr(expOrEle: Any): Any? {
        if (expOrEle is PsiExpression) {
            return resolveExpr(expOrEle)
        } else if (expOrEle is PsiElement) {
            return resolveExpr(expOrEle)
        }
        return expOrEle.toString()
    }

    fun resolveExpr(psiExpression: PsiExpression): Any? {
        when (psiExpression) {
            is PsiLiteralExpression -> return psiExpression.value
            is PsiReferenceExpression -> {
                val value = psiExpression.resolve()
                if (value != null) {
                    return resolveExpr(value)
                }
            }
            is JvmNamedElement -> return psiExpression.name
        }
        return psiExpression.text
    }

    fun resolveExpr(psiElement: PsiElement): Any? {
        when (psiElement) {
            is PsiVariable -> {
                val constantValue = psiElement.computeConstantValue()
                if (constantValue != null && constantValue != psiElement) {
                    return resolveExpr(constantValue)
                }
                return psiElement.name
            }
            is JvmNamedElement -> return psiElement.name
            is PsiExpression -> return resolveExpr(psiElement)
        }
        return psiElement.text
    }

    fun resolveFieldType(psiElement: PsiElement): Any? {
        when (psiElement) {
            is PsiField -> {
                return psiElement.type
            }
            is PsiMethod -> return psiElement.returnType
            is PsiExpression -> return resolveExpr(psiElement)
        }
        return psiElement.text
    }
}

fun <T> PsiElement?.docComment(action: ((PsiDocComment) -> T?)): T? {
    return this.cast(PsiDocCommentOwner::class)?.let { docCommentOwner ->
        ActionContext.getContext()!!.callInReadUI {
            docCommentOwner.docComment?.let { it -> action(it) }
        }
    }
}

fun PsiElement.visitChildren(acceptor: (PsiElement) -> Unit) {
    var current: PsiElement? = this.firstChild
    while (current != null) {
        acceptor(current)
        current = current.nextSibling
    }
}

fun PsiElement.findChildren(filter: (PsiElement) -> Boolean): PsiElement? {
    var current: PsiElement? = this.firstChild
    while (current != null) {
        if (filter(current)) {
            return current
        }
        current = current.nextSibling
    }
    return null
}
