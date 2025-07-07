package com.itangcent.common.utils

import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.charset.Charset

/**
 * Reads this stream completely as a String.
 *
 * *Note*: It is the caller's responsibility to close this reader.
 *
 * @return the string with corresponding file content.
 */
fun InputStream.readString(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).readText()
}

/**
 * Converts this string to a [URL] using URI parsing as an intermediate step.
 *
 * This function replaces the deprecated `URL(String)` constructor with the recommended approach
 * of using `URI` for better encoding handling and compliance with modern URL standards.
 *
 * Example usage:
 * ```
 * val url = "https://example.com/path?query=param".asUrl()
 * ```
 *
 * @throws IllegalArgumentException if the string contains invalid URI syntax
 * @throws java.net.MalformedURLException if the URI cannot be converted to a URL
 * @return [URL] object representing the string content
 */
fun String.asUrl(): URL {
    return URI(this).toURL()
}