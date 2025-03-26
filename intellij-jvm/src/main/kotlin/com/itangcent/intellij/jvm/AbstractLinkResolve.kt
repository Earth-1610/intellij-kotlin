package com.itangcent.intellij.jvm

import com.intellij.psi.*
import com.itangcent.intellij.context.ActionContext

abstract class AbstractLinkResolve : LinkResolver {

    override fun linkToPsiElement(plainText: String, linkTo: Any?): String? {
        return when (linkTo) {
            null -> linkToUnresolved(plainText)
            is PsiClass -> linkToClass(plainText, linkTo)
            is PsiMethod -> linkToMethod(plainText, linkTo)
            is PsiField -> linkToField(plainText, linkTo)
            is PsiType -> linkToType(plainText, linkTo)
            is PsiElement -> linkToOther(plainText, linkTo)
            else -> linkToUnresolved(plainText)
        }
    }

    open fun linkToUnresolved(plainText: String): String? {
        return plainText
    }

    open fun linkToType(plainText: String, linkType: PsiType): String? {
        return ActionContext.getContext()?.instance(JvmClassHelper::class)
            ?.resolveClassInType(linkType)?.let {
                linkToClass(plainText, it)
            }
    }

    abstract fun linkToClass(plainText: String, linkClass: PsiClass): String?

    abstract fun linkToMethod(plainText: String, linkMethod: PsiMethod): String?

    abstract fun linkToField(plainText: String, linkField: PsiField): String?

    open fun linkToOther(plainText: String, linkTo: PsiElement): String? {
        if (linkTo is PsiQualifiedNamedElement) {
            return linkTo.qualifiedName
        }
        if (linkTo is PsiNamedElement) {
            return linkTo.name
        }
        return plainText
    }
}