package com.itangcent.intellij.psi

import com.itangcent.intellij.util.computeIfAbsentSafely

abstract class AbstractClassRuleConfig : ClassRuleConfig {

    private var convertCache: MutableMap<String, String> = LinkedHashMap()

    override fun tryConvert(cls: String): String? {
        return convertCache.computeIfAbsentSafely(cls) {
            var origin = cls;
            var convertByRule = convertByRule(it)
            while (convertByRule != origin) {
                origin = convertByRule
                convertByRule = convertByRule(origin)
            }
            return@computeIfAbsentSafely convertByRule
        }
    }

    abstract fun convertByRule(cls: String): String
}