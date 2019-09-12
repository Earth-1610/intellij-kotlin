package com.itangcent.intellij.psi

import com.google.inject.ImplementedBy
import com.google.inject.Singleton


@ImplementedBy(DefaultClassRuleConfig::class)
interface ClassRuleConfig {

    /**
     * try convert one class to another for parse
     * @param cls class qualified name
     */
    fun tryConvert(cls: String): String?
}