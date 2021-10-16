package com.itangcent.intellij.jvm.spi

import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.safeComputeIfAbsent
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

object SpiBeanService {

    private val wrapCache: KV<KClass<*>, Any> = KV.create()

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> getBean(cls: KClass<S>): S? {
        return wrapCache.safeComputeIfAbsent(cls) {
            createProxy(cls)
        } as? S
    }

    @Suppress("UNCHECKED_CAST")
    private fun <S : Any> createProxy(cls: KClass<S>): S? {
        val loadServices = SpiUtils.loadServices(cls) ?: return null
        return Proxy.newProxyInstance(
            cls.java.classLoader, arrayOf(cls.java),
            ContextProxyBean(loadServices.toTypedArray())
        ) as S
    }
}