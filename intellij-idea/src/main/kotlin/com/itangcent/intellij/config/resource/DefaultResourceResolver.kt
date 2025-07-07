package com.itangcent.intellij.config.resource

import com.google.inject.Singleton
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceWarn
import com.itangcent.common.utils.asUrl
import java.net.MalformedURLException
import java.net.URISyntaxException

@Singleton
open class DefaultResourceResolver : ResourceResolver {

    companion object : Log()

    /**
     * Resolves a resource from the given URL or file path.
     * @param url The URL or file path to resolve
     * @return The resolved Resource
     * @throws IllegalArgumentException if the URL is malformed or resource is not reachable
     * @throws NullPointerException if the input URL is null
     */

    override fun resolve(url: String): Resource {
        require(url.isNotBlank()) { "URL must not be blank" }

        LOG.debug("Attempting to resolve resource: $url")

        if (!url.contains(':')) {
            return createFileResource(url)
        }

        try {
            return createUrlResource(url)
        } catch (_: MalformedURLException) {
            LOG.warn("Failed to parse URL: $url")
        } catch (_: URISyntaxException) {
            LOG.warn("Failed to parse URL: $url")
        } catch (e: Exception) {
            LOG.traceWarn("Unexpected error while processing URL: $url", e)
        }

        try {
            return createFileResource(url).takeIf { it.reachable }
                ?: throw IllegalArgumentException("Resource is not reachable: $url")
        } catch (e: Exception) {
            LOG.traceWarn("Failed to access file resource: $url", e)
        }

        throw IllegalArgumentException("Failed to access resource: $url")
    }

    /**
     * Creates a URL resource from the given URL string.
     * @param url The URL string
     * @return URLResource instance
     * @throws MalformedURLException if the URL is invalid
     */
    protected open fun createUrlResource(url: String): URLResource {
        return URLResource(url.asUrl())
    }

    /**
     * Creates a file resource from the given file path.
     * @param url The file path
     * @return FileResource instance
     * @throws IllegalArgumentException if the file path is invalid
     */
    protected open fun createFileResource(url: String): FileResource {
        require(url.isNotBlank()) { "File path must not be blank" }
        return FileResource(url)
    }
}
