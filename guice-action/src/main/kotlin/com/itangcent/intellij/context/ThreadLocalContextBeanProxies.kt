package com.itangcent.intellij.context

import com.itangcent.intellij.constant.EventKey
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.cast


object ThreadLocalContextBeanProxies {

    /**
     * clazz should be interface
     */
    fun <T : Any> instance(clazz: KClass<T>): T {
        val loader = clazz.java.classLoader
        val interfaces = arrayOf(clazz.java)
        return clazz.cast(Proxy.newProxyInstance(loader, interfaces,
            LazyInitInvocationHandler(clazz)
        ))
    }

    class LazyInitInvocationHandler<T : Any>(private var clazz: KClass<T>) : InvocationHandler {

        private var localCache: ThreadLocal<WeakReference<T>> = ThreadLocal()

        private var localBean: T? = null
            get() {
                var weakCacheBean = localCache.get()
                var cacheBean: T?
                if (weakCacheBean != null) {
                    cacheBean = weakCacheBean.get()
                    if (cacheBean != null) {
                        return cacheBean
                    }
                }

                val actionContext = ActionContext.getContext()
                cacheBean = actionContext!!.instance(clazz)
                weakCacheBean = WeakReference(cacheBean)
                localCache.set(weakCacheBean)
                actionContext.on(EventKey.ON_COMPLETED) {
                    weakCacheBean.clear()
                }

                return cacheBean
            }


        override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
            return when (args) {
                null -> method!!.invoke(localBean)
                else -> method!!.invoke(localBean, *args)
            }
        }

    }
}