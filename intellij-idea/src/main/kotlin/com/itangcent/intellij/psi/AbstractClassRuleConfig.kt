package com.itangcent.intellij.psi

import com.itangcent.intellij.util.computeIfAbsentSafely

abstract class AbstractClassRuleConfig : ClassRuleConfig {

    private var convertCache: MutableMap<String, String> = LinkedHashMap()

    override fun tryConvert(cls: String): String? {

        return convertCache.computeIfAbsentSafely(cls) {
            convertByRule(it)
        }
    }

    abstract fun convertByRule(cls: String): String
}