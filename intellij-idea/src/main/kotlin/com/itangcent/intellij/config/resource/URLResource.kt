package com.itangcent.intellij.config.resource

import com.itangcent.common.utils.unsafe
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

open class URLResource : Resource {

    constructor(uri: URI) : super() {
        this.url = uri.toURL()
    }

    constructor(url: URL) : super() {
        this.url = url
    }

    final override val url: URL

    override val inputStream: InputStream?
        get() {
            var connection = url.openConnection()
            if (connection is HttpsURLConnection) {//support https
                connection = connection.unsafe()
            }
            onConnection(connection)
            return connection.inputStream
        }

    protected open fun onConnection(connection: URLConnection) {
        connection.connectTimeout = DEFAULT_TIME_OUT
    }
}

/**
 * The default timeout value in milliseconds to be used
 * for opening the URLConnection.
 */
var DEFAULT_TIME_OUT = TimeUnit.SECONDS.toMillis(30).toInt()
