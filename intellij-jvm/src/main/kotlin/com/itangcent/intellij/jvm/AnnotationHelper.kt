package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.standard.StandardAnnotationHelper

@ImplementedBy(StandardAnnotationHelper::class)
interface AnnotationHelper {

    fun findAnnMap(psiElement: PsiElement?, annName: String): Map<String, Any?>?

    fun hasAnn(psiElement: PsiElement?, annName: String): Boolean

    fun findAttr(psiElement: PsiElement?, annName: String): Any?

    fun findAttr(psiElement: PsiElement?, annName: String, vararg attrs: String): Any?

    fun findAttrAsString(psiElement: PsiElement?, annName: String): String?

    fun findAttrAsString(psiElement: PsiElement?, annName: String, vararg attrs: String): String?

}