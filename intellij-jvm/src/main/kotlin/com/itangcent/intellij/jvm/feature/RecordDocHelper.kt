package com.itangcent.intellij.jvm.feature

import com.google.inject.Singleton
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.impl.light.LightRecordField
import com.itangcent.common.utils.appendln
import com.itangcent.intellij.jvm.standard.StandardDocHelper

@Singleton
class RecordDocHelper : StandardDocHelper() {

    override fun getAttrOfField(field: PsiField): String? {
        if (field !is LightRecordField) {
            return null
        }
        val docFromClassParams = findDocsByTagAndName(field.containingClass, "param", field.name)
        return docFromClassParams.appendln(super.getAttrOfField(field))
    }

    override fun getEolComment(psiElement: PsiElement): String? {
        if (psiElement !is LightRecordField) {
            return null
        }

        psiElement.navigationElement.prevSiblings()
            .findBlockComment()
            ?.let {
                return it
            }

        psiElement.navigationElement.nextSiblings()
            .findEolComment { !it.isComma() }
            ?.let {
                return it
            }

        return null
    }

    private fun PsiElement.isComma(): Boolean {
        return this is PsiJavaToken && this.tokenType == JavaTokenType.COMMA
    }
}
