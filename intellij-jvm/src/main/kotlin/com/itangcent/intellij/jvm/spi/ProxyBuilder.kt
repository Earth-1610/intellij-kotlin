package com.itangcent.intellij.jvm.spi

import com.itangcent.common.logger.ILogger
import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.privileged
import com.itangcent.intellij.context.ActionContext
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

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
    private val delegateBuilders: LinkedList<DelegateBuilder> = LinkedList()

    fun addImplementClass(implementClass: KClass<*>) {
        if (implementClasses.contains(implementClass)) {
            logger?.warn("add implement class repeatedly:${implementClass.qualifiedName}")
        }
        delegateBuilders.add(createDelegateBuilder(implementClass))
    }

    private fun createDelegateBuilder(implementClass: KClass<*>): DelegateBuilder {
        val noArgsConstructor = implementClass.constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
        if (noArgsConstructor != null) {
            return SingleDelegateBuilder(noArgsConstructor)
        }

        val oneArgsConstructor = implementClass.constructors.singleOrNull {
            it.parameters.size == 1
        }
        if (oneArgsConstructor != null) {
            return ConstructorDelegateBuilder(oneArgsConstructor)
        }

        throw IllegalArgumentException("noArgsConstructor/oneArgsConstructor was required for ${implementClass.qualifiedName}")
    }

    companion object {
        private var logger: ILogger? = SpiUtils.loadService(ILogger::class)
    }

    private fun buildProxy(): Any {
        val delegates: Array<Any?> = kotlin.Array(implementClasses.size) { null }
        for ((index, delegateBuilder) in delegateBuilders.withIndex()) {
            delegates[index] = delegateBuilder.buildInstance(delegates)
        }
        return Proxy.newProxyInstance(
            injectClass.java.classLoader, arrayOf(injectClass.java),
            ProxyBean(delegates.requireNoNulls().reversedArray())
        )
    }

    @Volatile
    private var injected: Boolean = false

    @Suppress("UNCHECKED_CAST")
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

interface DelegateBuilder {
    fun buildInstance(delegates: Array<Any?>): Any
}

class SingleDelegateBuilder(private val constructor: KFunction<Any>) : DelegateBuilder {
    override fun buildInstance(delegates: Array<Any?>): Any {
        return constructor.callBy(emptyMap())
    }
}

class ConstructorDelegateBuilder(private val constructor: KFunction<Any>) : DelegateBuilder {
    override fun buildInstance(delegates: Array<Any?>): Any {
        return constructor.call(delegates.first()!!)
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