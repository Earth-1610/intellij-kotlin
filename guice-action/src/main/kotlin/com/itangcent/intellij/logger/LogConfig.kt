package com.itangcent.intellij.logger

import com.google.inject.Singleton
import java.nio.charset.Charset

@Singleton
open class LogConfig {

    open fun charset(): Charset {
        return Charsets.UTF_8
    }
}