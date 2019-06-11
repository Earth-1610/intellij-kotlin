package com.itangcent.intellij.psi

import com.intellij.psi.PsiField

interface ClassRuleConfig {

    /**
     * try convert one class to another for parse
     * @param cls class qualified name
     */
    fun tryConvert(cls: String): String?

    /**
     * gets the comment of the field
     */
    fun findDoc(field: PsiField): String?

    fun getFieldName(field: PsiField): String?

    fun ignoreField(field: PsiField): Boolean?
}