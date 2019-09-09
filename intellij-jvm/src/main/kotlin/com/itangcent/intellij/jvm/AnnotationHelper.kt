package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.intellij.psi.PsiMember
import com.itangcent.intellij.jvm.standard.StandardAnnotationHelper

@ImplementedBy(StandardAnnotationHelper::class)
interface AnnotationHelper {

    fun findAnnMap(psiMember: PsiMember?, annName: String): Map<String, Any?>?

    fun hasAnn(psiMember: PsiMember?, annName: String): Boolean

    fun findAttr(psiMember: PsiMember?, annName: String): String?

    fun findAttr(psiMember: PsiMember?, annName: String, vararg attrs: String): String?

}