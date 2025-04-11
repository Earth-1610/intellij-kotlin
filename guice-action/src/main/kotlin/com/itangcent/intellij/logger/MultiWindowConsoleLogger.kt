package com.itangcent.intellij.logger

import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import com.itangcent.intellij.utils.ApplicationUtils

/**
 * A factory that provides the appropriate MultiWindowConsoleLogger implementation
 * based on the IDE version:
 * - For IDE versions less than 2023: Uses MultiToolWindowConsoleLogger
 * - For IDE versions 2023 and above: Uses MultiContentConsoleLogger
 */
@Singleton
class MultiWindowConsoleLogger : Logger {

    @Inject
    private lateinit var multiToolWindowConsoleLoggerProvider: Provider<MultiToolWindowConsoleLogger>

    @Inject
    private lateinit var multiContentConsoleLoggerProvider: Provider<MultiContentConsoleLogger>

    private val delegateLogger by lazy {
        if (ApplicationUtils.isIdeaVersionLessThan2023) {
            multiToolWindowConsoleLoggerProvider.get()
        } else {
            multiContentConsoleLoggerProvider.get()
        }
    }

    override fun log(level: Level, msg: String) {
        delegateLogger.log(level, msg)
    }
} 