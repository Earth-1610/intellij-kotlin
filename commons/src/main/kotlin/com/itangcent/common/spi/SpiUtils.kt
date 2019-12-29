package com.itangcent.common.spi

import java.util.*
import kotlin.reflect.KClass

object SpiUtils {

    fun <S : Any> loadService(
        service: KClass<S>
    ): S? {
        return loadService(service, SpiUtils::class.java.classLoader)
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