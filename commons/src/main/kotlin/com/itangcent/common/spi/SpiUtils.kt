package com.itangcent.common.spi

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.ILoggerProvider
import com.itangcent.common.utils.notNullOrEmpty
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object SpiUtils {

    private val serviceCache = ConcurrentHashMap<Any, Any?>()

    private val NON = Object()

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> setService(
        service: KClass<S>,
        serviceBean: S
    ) {
        serviceCache[service] = serviceBean
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> loadService(
        service: KClass<S>
    ): S? {
        return serviceCache.computeIfAbsent(service) {
            loadService(service, SpiUtils::class.java.classLoader) ?: NON
        }?.takeIf { it !== NON } as S?
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> loadServices(
        service: KClass<S>
    ): List<S>? {
        return serviceCache.computeIfAbsent("list-" + service.qualifiedName!!) {
            loadServices(service, SpiUtils::class.java.classLoader) ?: NON
        }?.takeIf { it !== NON } as List<S>?
    }

    fun <S : Any> loadService(
        service: KClass<S>,
        loader: ClassLoader
    ): S? {
        val serviceInstance = ServiceLoader.load(
            service.java,
            loader
        ).firstOrNull()
        if (!service.isSubclassOf(ILoggerProvider::class)) {
            LOG?.info("load service ${service.qualifiedName}:$serviceInstance")
        }
        return serviceInstance
    }

    fun <S : Any> loadServices(
        service: KClass<S>,
        loader: ClassLoader
    ): List<S>? {
        val serviceInstance = ServiceLoader.load(
            service.java,
            loader
        ).toList()
        if (!service.isSubclassOf(ILoggerProvider::class)) {
            LOG?.info("load services ${service.qualifiedName}:$serviceInstance")
        }
        return serviceInstance.takeIf { it.notNullOrEmpty() }
    }
}

//background idea log
private val LOG: ILogger? = SpiUtils.loadService(ILoggerProvider::class)?.getLogger(SpiUtils::class)
