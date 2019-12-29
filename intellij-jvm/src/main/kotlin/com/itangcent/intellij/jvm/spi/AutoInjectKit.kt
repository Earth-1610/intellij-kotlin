package com.itangcent.intellij.jvm.spi

import com.google.inject.ImplementedBy
import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.KV
import com.itangcent.common.utils.safeComputeIfAbsent
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.withUnsafe
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object AutoInjectKit {

    private var logger: ILogger? = SpiUtils.loadService(ILogger::class)

    fun tryLoad(
        classLoader: ClassLoader,
        bindClassName: String
    ): KClass<*>? {
        return try {
            val loadClass: Class<*> = classLoader.loadClass(bindClassName)
            loadClass.constructors
            loadClass.methods
            logger?.log("load:$bindClassName success")
            loadClass.kotlin
        } catch (e: Throwable) {
            logger?.traceError("error load:$bindClassName ", e)
            null
        }
    }

    fun tryLoadAndBind(
        classLoader: ClassLoader,
        injectClass: KClass<*>,
        bindClassName: String
    ) {
        try {
            val bindClass =
                tryLoad(classLoader, bindClassName) ?: return

            ActionContext.addDefaultInject { actionContextBuilder ->
                actionContextBuilder.bind(injectClass) {
                    it.withUnsafe(bindClass)
                }
            }

            logger?.log("load:$bindClassName and bind")
        } catch (e: Throwable) {
            logger?.traceError("load:$bindClassName and bind failed ", e)
        }
    }

    private val wrapCache: KV<KClass<*>, ProxyBuilder> = KV.create()

    fun tryLoadAndWrap(
        classLoader: ClassLoader,
        injectClass: KClass<*>,
        bindClassName: String
    ) {
        try {
            val bindClass =
                tryLoad(classLoader, bindClassName) ?: return

            val proxyBuilder = wrapCache.safeComputeIfAbsent(injectClass) {
                createProxyBuilder(classLoader, injectClass)
            }
            proxyBuilder!!.addImplementClass(bindClass)
            logger?.log("load:$bindClassName and wrap")
        } catch (e: Throwable) {
            logger?.traceError("load:$bindClassName and wrap failed ", e)
        }
    }

    private fun createProxyBuilder(classLoader: ClassLoader, injectClass: KClass<*>): ProxyBuilder {
        val proxyBuilder = ProxyBuilder(injectClass, classLoader)
        wrapCache[injectClass] = proxyBuilder
        proxyBuilder.injectByDefault()
        val implementedBy = injectClass.findAnnotation<ImplementedBy>()
        val defaultImplementedBy = implementedBy?.value
        if (defaultImplementedBy != null) {
            proxyBuilder.addImplementClass(defaultImplementedBy)
        }
        return proxyBuilder
    }
}