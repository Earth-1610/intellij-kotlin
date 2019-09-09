package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiMember
import com.itangcent.intellij.jvm.standard.StandardDocHelper

@ImplementedBy(StandardDocHelper::class)
interface DocHelper {

    fun getTagMapOfDocComment(psiMember: PsiMember?): Map<String, String?>

    fun getAttrOfDocComment(psiMember: PsiMember?): String?

    fun findDocsByTagAndName(psiMember: PsiMember?, tag: String, name: String): String?

    fun findDocsByTag(psiMember: PsiMember?, tag: String?): String?

    fun findDocByTag(psiMember: PsiMember?, tag: String?): String?

    fun hasTag(psiMember: PsiMember?, tag: String?): Boolean
}