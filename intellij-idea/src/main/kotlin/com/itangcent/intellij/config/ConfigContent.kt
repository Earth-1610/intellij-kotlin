package com.itangcent.intellij.config

import com.itangcent.intellij.config.resource.Resource

interface ConfigContent {
    val id: String

    val content: String

    val type: String
}

data class ConfigContentData(
    override val content: String,
    override val type: String,
) : ConfigContent {
    override val id: String
        get() = "Data:" + content.hashCode().toString()
}

fun ConfigContent(content: String, type: String): ConfigContent = ConfigContentData(content, type)

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
}

fun ConfigContent(resource: Resource): ConfigContent = ResourceConfigContent(resource)
