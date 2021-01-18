package com.itangcent.common.spi

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object SpiUtils {

    private val serviceCache = ConcurrentHashMap<KClass<*>, Any?>()

    private val NON = Object()

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> loadService(
        service: KClass<S>
    ): S? {
        return serviceCache.computeIfAbsent(service) {
            loadService(service, SpiUtils::class.java.classLoader) ?: NON
        }?.takeIf { it !== NON } as S?
    }

    fun <S : Any> loadService(
        service: KClass<S>,
        loader: ClassLoader
    ): S? {
        return ServiceLoader.load(
            service.java,
            loader
        ).firstOrNull()
    }
}