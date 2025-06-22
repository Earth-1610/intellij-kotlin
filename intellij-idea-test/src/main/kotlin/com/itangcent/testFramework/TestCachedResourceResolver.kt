package com.itangcent.testFramework

import com.itangcent.common.logger.Log
import com.itangcent.common.utils.asUrl
import com.itangcent.intellij.config.resource.DefaultResourceResolver
import com.itangcent.intellij.config.resource.URLResource
import com.itangcent.intellij.file.DefaultLocalFileRepository
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

class TestCachedResourceResolver : DefaultResourceResolver() {

    companion object : Log()

    private var cacheRepository = DefaultLocalFileRepository()

    override fun createUrlResource(url: String): URLResource {
        return CachedURLResource(url.asUrl())
    }

    open inner class CachedURLResource(url: URL) : URLResource(url) {

        private fun loadCache(): ByteArray? {
            val cacheFileName = url.toString().replace('/', '_')
            val cached = cacheRepository.getFile(cacheFileName)
            if (cached != null && cached.exists()) {
                return cached.readBytes()
            }
            val bytes = super.inputStream?.use { it.readBytes() }
            bytes?.let {
                val cacheFile = cacheRepository.getOrCreateFile(cacheFileName)
                cacheFile.writeBytes(it)
                LOG.info("cache $url to $cacheFile")
            }
            return bytes
        }

        override val inputStream: InputStream?
            get() {
                val valueBytes = loadCache()
                return valueBytes?.let { ByteArrayInputStream(it) }
            }

        override val bytes: ByteArray?
            get() = loadCache()

        override val content: String?
            get() = loadCache()?.let { String(it, Charsets.UTF_8) }

        override fun onConnection(connection: URLConnection) {
            //set timeout 6000 by default
            connection.connectTimeout = 6000
        }
    }
}