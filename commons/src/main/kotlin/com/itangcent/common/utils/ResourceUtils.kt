package com.itangcent.common.utils

import java.net.URL

object ResourceUtils {

    private val resourceCache: KV<String, String> = KV()

    fun readResource(resourceName: String): String {
        return resourceCache.safeComputeIfAbsent(resourceName) {
            (ResourceUtils::class.java.classLoader.getResourceAsStream(resourceName)
                ?: ResourceUtils::class.java.getResourceAsStream(resourceName))
                ?.use {
                    it.readString(Charsets.UTF_8)
                } ?: ""
        } ?: ""
    }

    private val resourceURLCache: KV<String, URL> = KV()

    fun findResource(resourceName: String): URL? {
        return resourceURLCache.safeComputeIfAbsent(resourceName) {
            doFindResource(resourceName)
        }
    }

    private fun doFindResource(resourceName: String): URL {
        return ResourceUtils::class.java.classLoader.getResource(resourceName)
            ?: ResourceUtils::class.java.getResource(resourceName)!!
    }
}
