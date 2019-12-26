package com.itangcent.intellij.jvm.spi

import com.itangcent.common.logger.ILogger
import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.privileged
import com.itangcent.intellij.context.ActionContext
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class ProxyBuilder {

    private val injectClass: KClass<*>
    private val classLoader: ClassLoader

    constructor(injectClass: KClass<*>) {
        this.injectClass = injectClass
        this.classLoader = injectClass.java.classLoader
    }

    constructor(injectClass: KClass<*>, classLoader: ClassLoader) {
        this.injectClass = injectClass
        this.classLoader = classLoader
    }

    private val implementClasses: LinkedList<KClass<*>> = LinkedList()

    fun addImplementClass(implementClass: KClass<*>) {
        if (implementClasses.contains(implementClass)) {
            logger?.warn("add implement class repeatedly:${implementClass.qualifiedName}")
        }
        implementClasses.addFirst(implementClass)
    }

    companion object {
        private var logger: ILogger? = SpiUtils.loadService(ILogger::class)
    }

    private fun buildProxy(): Any {
        val delegates = implementClasses.map { it.createInstance() }
            .toTypedArray()
        return Proxy.newProxyInstance(
            injectClass.java.classLoader, arrayOf(injectClass.java),
            ProxyBean(delegates)
        )
    }

    @Volatile
    private var injected: Boolean = false

    fun injectByDefault() {
        if (!injected) {
            injected = true
            ActionContext.addDefaultInject { actionContextBuilder ->
                val buildProxy = this.buildProxy()
                actionContextBuilder.bindInstance(injectClass as KClass<Any>, buildProxy)
            }
        }
    }
}

class ProxyBean(private val delegates: Array<Any>) : InvocationHandler {

    @Volatile
    private var initiated: Int = 0

    private fun checkInit() {
        if (initiated == 0) {
            synchronized(this) {
                if (initiated == 0) {
                    val context = ActionContext.getContext()
                    if (context != null) {
                        for (delegate in delegates) {
                            context.init(delegate)
                        }
                        initiated = 1
                    }
                }
            }

        }
    }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        return invoke(method, args ?: emptyArray())
    }

    fun invoke(method: Method, args: Array<out Any>): Any? {
        checkInit()
        var availableRet: Any? = null
        for (delegate in delegates) {
            try {
                val ret = method.privileged { it.invoke(delegate, *args) }
                if (ret != null) {
                    if (ret.isInvalidResult()) {
                        availableRet = ret
                        continue
                    } else {
                        return ret
                    }
                }
            } catch (e: Throwable) {
                //ignore NotImplemented
                if (e.isNotImplemented()) {
                    continue
                }
                throw e
            }
        }
        return availableRet
    }

    private fun Throwable.isNotImplemented(): Boolean {
        return when {
            this is UndeclaredThrowableException -> this.undeclaredThrowable.isNotImplemented()
            this is InvocationTargetException -> this.targetException.isNotImplemented()
            else -> this is NotImplementedError
        }
    }

    private fun Any?.isInvalidResult(): Boolean {
        return when {
            this == null -> true
            this is Boolean -> !this
            this is Number -> this.toDouble() != 0.0
            this is String && this.isEmpty() -> true
            this is Array<*> && this.isEmpty() -> true
            this is Collection<*> && this.isEmpty() -> true
            this is Map<*, *> && this.isEmpty() -> true
            else -> false
        }
    }
}