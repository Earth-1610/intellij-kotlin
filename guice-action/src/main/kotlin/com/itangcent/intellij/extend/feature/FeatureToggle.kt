package com.itangcent.intellij.extend.feature

/**
 * @author tangcent
 * @date 2024/09/26
 */
interface FeatureToggle {
    fun isFeatureDisabled(key: String): Boolean
}

object NoFeatureToggle : FeatureToggle {
    override fun isFeatureDisabled(key: String): Boolean {
        return false
    }
}