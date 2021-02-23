package com.itangcent.intellij.config.resource

import com.google.inject.Singleton
import java.net.MalformedURLException
import java.net.URL

@Singleton
open class DefaultResourceResolver : ResourceResolver {

    override fun resolve(url: String): Resource {
        if (!url.contains(':')) {
            return createFileResource(url)
        }
        return try {
            createUrlResource(url)
        } catch (e: MalformedURLException) {
            createFileResource(url)
        }
    }

    protected open fun createUrlResource(url: String) = URLResource(URL(url))

    protected open fun createFileResource(url: String): FileResource {
        return FileResource(url)
    }
}
