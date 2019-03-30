package com.itangcent.intellij.psi

import com.intellij.psi.PsiField

interface ClassRuleConfig {

    /**
     * 尝试将一个类转换为另一个类来处理
     * @param cls 类名
     */
    fun tryConvert(cls: String): String?

    /**
     * 获取字段上的注释
     */
    fun findDoc(field: PsiField): String?

    fun getFieldName(field: PsiField): String?
}