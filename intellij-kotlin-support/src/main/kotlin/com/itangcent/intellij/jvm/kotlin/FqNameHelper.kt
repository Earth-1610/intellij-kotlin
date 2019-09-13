package com.itangcent.intellij.jvm.kotlin

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.inject.Singleton
import org.jetbrains.kotlin.name.FqName

@Singleton
class FqNameHelper {

    private val fqNameCache: LoadingCache<String, FqName> =
        CacheBuilder.newBuilder()
            .build<String, FqName>(CacheLoader.from { name -> FqName(name!!) })

    fun of(fqName: String): FqName {
        return fqNameCache.get(fqName)
    }
}