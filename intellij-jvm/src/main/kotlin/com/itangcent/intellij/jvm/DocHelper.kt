package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiElement
import com.itangcent.intellij.jvm.standard.StandardDocHelper

@ImplementedBy(StandardDocHelper::class)
interface DocHelper {

    fun getTagMapOfDocComment(psiElement: PsiElement?): Map<String, String?>

    fun getSubTagMapOfDocComment(psiElement: PsiElement?, tag: String): Map<String, String?>

    fun getAttrOfDocComment(psiElement: PsiElement?): String?

    fun findDocsByTagAndName(psiElement: PsiElement?, tag: String, name: String): String?

    fun findDocsByTag(psiElement: PsiElement?, tag: String?): List<String>?

    fun findDocByTag(psiElement: PsiElement?, tag: String?): String?

    fun hasTag(psiElement: PsiElement?, tag: String?): Boolean
}