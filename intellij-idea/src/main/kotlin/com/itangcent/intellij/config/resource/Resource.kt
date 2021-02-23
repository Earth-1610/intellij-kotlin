package com.itangcent.intellij.config.resource

import com.itangcent.common.utils.readString
import java.io.InputStream
import java.net.URL

abstract class Resource {

    abstract val url: URL

    open val reachable: Boolean
        get() = true

    open val inputStream: InputStream?
        get() {
            return url.openStream()
        }

    open val bytes: ByteArray?
        get() {
            return inputStream?.use { it.readBytes() }
        }

    open val content: String?
        get() {
            return inputStream?.use { it.readString() }
        }

    override fun toString(): String {
        return url.toString()
    }
}
