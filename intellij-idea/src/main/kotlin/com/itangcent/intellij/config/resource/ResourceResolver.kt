package com.itangcent.intellij.config.resource

import com.google.inject.ImplementedBy

@ImplementedBy(DefaultResourceResolver::class)
interface ResourceResolver {
    fun resolve(url: String): Resource
}
