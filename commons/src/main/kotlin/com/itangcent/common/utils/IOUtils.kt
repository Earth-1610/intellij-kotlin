package com.itangcent.common.utils

import java.io.Closeable

object IOUtils {

    fun closeQuietly(vararg closeables: Closeable?) {
        for (closeable in closeables) {
            try {
                closeable?.close()
            } catch (_: Exception) {
            }
        }
    }

}
