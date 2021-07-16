package com.itangcent.intellij.config.resource

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.ILoggerProvider
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SpiUtils
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
            try {
                return connection.inputStream
            } catch (e: Exception) {
                LOG?.traceError("failed fetch:[$url]", e)
                throw e
            }
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

private val LOG: ILogger? = SpiUtils.loadService(ILoggerProvider::class)?.getLogger(URLResource::class)
