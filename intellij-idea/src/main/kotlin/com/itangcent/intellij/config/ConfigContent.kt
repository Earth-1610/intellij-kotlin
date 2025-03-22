package com.itangcent.intellij.config

import com.itangcent.intellij.config.resource.Resource

interface ConfigContent {
    val id: String

    val content: String

    val type: String

    val url: String?
}

data class RawConfigContentData(
    override val content: String,
    override val type: String,
) : ConfigContent {
    override val id: String
        get() = "Data:" + content.hashCode().toString()
    
    override val url: String?
        get() = null
}

data class UrlConfigContent(
    override val content: String,
    override val type: String,
    override val url: String?,
) : ConfigContent {
    override val id: String
        get() = "Url:" + (url ?: content.hashCode().toString())
}

fun ConfigContent(content: String, type: String): ConfigContent = RawConfigContentData(content, type)
fun ConfigContent(content: String, type: String, url: String?): ConfigContent = UrlConfigContent(content, type, url)

class ResourceConfigContent(
    private val resource: Resource
) : ConfigContent {
    override val id: String
        get() = "Resource:" + resource.url.toString()

    override val content: String by lazy {
        resource.content ?: ""
    }

    override val type: String by lazy {
        resource.url.file.substringAfterLast('.')
    }

    override val url: String?
        get() = resource.url.toString()
}

fun ConfigContent(resource: Resource): ConfigContent = ResourceConfigContent(resource)
