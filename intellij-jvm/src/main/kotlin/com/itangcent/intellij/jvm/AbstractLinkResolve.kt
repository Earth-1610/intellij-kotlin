package com.itangcent.intellij.jvm

import com.intellij.psi.*

public abstract class AbstractLinkResolve : LinkResolver {
    override fun linkToPsiElement(plainText: String, linkTo: PsiElement?): String? {
        return when (linkTo) {
            null -> linkToUnresolved(plainText)
            is PsiClass -> linkToClass(plainText, linkTo)
            is PsiMethod -> linkToMethod(plainText, linkTo)
            is PsiField -> linkToField(plainText, linkTo)
            else -> linkToOther(plainText, linkTo)
        }
    }

    open fun linkToUnresolved(plainText: String): String? {
        return plainText
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