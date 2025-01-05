package com.itangcent.intellij.utils

import com.intellij.openapi.application.ApplicationInfo

object ApplicationUtils {
    val isIdeaVersionLessThan2023: Boolean
        get() {
            val majorVersion = ApplicationInfo.getInstance().majorVersion.toIntOrNull() ?: return false
            return majorVersion < 2023
        }
} 