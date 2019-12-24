package com.itangcent.intellij.jvm

import com.google.inject.ImplementedBy
import com.itangcent.common.utils.ClassHelper
import com.itangcent.common.utils.KV
import com.itangcent.intellij.constant.EventKey
import com.itangcent.intellij.context.ActionContext
import com.itangcent.intellij.extend.guice.withUnsafe
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

object AutoInjectKit {

    fun tryLoad(
        classLoader: ClassLoader,
        bindClassName: String
    ) {
        try {
            classLoader.loadClass(bindClassName)
        } catch (ignore: Exception) {
        }
    }

    fun tryLoadAndBind(
        classLoader: ClassLoader,
        injectClass: KClass<*>,
        bindClassName: String
    ) {
        try {
            val bindClass =
                classLoader.loadClass(bindClassName).kotlin

            ActionContext.addDefaultInject { actionContextBuilder ->
                actionContextBuilder.bind(injectClass) {
                    it.withUnsafe(bindClass)
                }
            }
        } catch (e: Exception) {
        }
    }

    private val wrapCache: KV<KClass<*>, Any> = KV.create()

    fun tryLoadAndWrap(
        classLoader: ClassLoader,
        injectClass: KClass<*>,
        bindClassName: String
    ) {
        try {
            val bindClass =
                classLoader.loadClass(bindClassName).kotlin

            var cachedWrap = wrapCache[injectClass]
            if (cachedWrap == null) {
                val implementedBy = injectClass.findAnnotation<ImplementedBy>()
                val defaultImplementedBy = implementedBy?.value
                if (defaultImplementedBy == null) {
                    wrap(injectClass, bindClass)
                    return
                } else {
                    cachedWrap = defaultImplementedBy.createInstance()
                    initOnStart(cachedWrap)
                }
            }
            wrap(injectClass, bindClass, cachedWrap)
        } catch (e: Exception) {
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun wrap(injectClass: KClass<*>, bindClass: KClass<*>) {
        val instance = bindClass.createInstance()
        wrapCache[injectClass] = instance
        initOnStart(instance)
        ActionContext.addDefaultInject { actionContextBuilder ->
            actionContextBuilder.bindInstance(injectClass as KClass<Any>, instance)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun wrap(injectClass: KClass<*>, bindClass: KClass<*>, oldWrap: Any) {
        val instance = ClassHelper.newInstance(bindClass, oldWrap)
        wrapCache[injectClass] = instance
        initOnStart(instance)
        ActionContext.addDefaultInject { actionContextBuilder ->
            actionContextBuilder.bindInstance(injectClass as KClass<Any>, instance)
        }
    }

    private fun initOnStart(cachedWrap: Any) {
        ActionContext.addDefaultInject { actionContextBuilder ->
            actionContextBuilder.addAction {
                it.on(EventKey.ON_START) { context ->
                    context.init(cachedWrap)
                }
            }
        }
    }

}
