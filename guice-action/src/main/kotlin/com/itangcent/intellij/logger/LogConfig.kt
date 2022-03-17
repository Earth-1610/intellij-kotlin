package com.itangcent.intellij.logger

import com.google.inject.Singleton
import java.nio.charset.Charset

/**
 * Log config for console output
 *
 * @author tangcent
 */
@Singleton
open class LogConfig {

    open fun charset(): Charset {
        return Charsets.UTF_8
    }
}