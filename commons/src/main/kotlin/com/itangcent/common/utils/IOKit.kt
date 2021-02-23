package com.itangcent.common.utils

import java.io.InputStream
import java.nio.charset.Charset

/**
 * Reads this stream completely as a String.
 *
 * *Note*:  It is the caller's responsibility to close this reader.
 *
 * @return the string with corresponding file content.
 */
fun InputStream.readString(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).readText()
}

