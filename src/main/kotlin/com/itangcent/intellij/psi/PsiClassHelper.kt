package com.itangcent.intellij.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.itangcent.intellij.util.KV

interface PsiClassHelper {
    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?>

    fun isNormalType(typeName: String): Boolean

    fun copy(obj: Any?): Any?

    fun unboxArrayOrList(psiType: PsiType): PsiType

    fun getDefaultValue(typeName: String): Any?
    
    fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement?
}