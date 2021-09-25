package com.itangcent.intellij.jvm.string

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiType
import com.itangcent.common.string.GracefulToString

class PsiClassGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        return (any as? PsiClass)?.qualifiedName
    }
}

class PsiTypeGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        return (any as? PsiType)?.canonicalText
    }
}

class PsiMemberGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        val psiMember = any as? PsiMember ?: return null
        val className = psiMember.containingClass?.name ?: return null
        return className + "#" + psiMember.name
    }
}

class PsiElementGracefulToString : GracefulToString {
    override fun toString(any: Any?): String? {
        return (any as? PsiElement)?.text
    }
}