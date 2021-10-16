package com.itangcent.intellij.jvm.spi

import com.itangcent.common.logger.ILogger
import com.itangcent.common.spi.ProxyBean
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
        implementClasses.add(implementClass)
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

    fun buildProxy(): Any {
        val delegates: Array<Any?> = Array(delegateBuilders.size) { null }
        for ((index, delegateBuilder) in delegateBuilders.withIndex()) {
            delegates[index] = delegateBuilder.buildInstance(delegates)
        }
        return Proxy.newProxyInstance(
            injectClass.java.classLoader, arrayOf(injectClass.java),
            ContextProxyBean(delegates.requireNoNulls().reversedArray())
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

class ContextProxyBean(private val delegates: Array<Any>) : ProxyBean(delegates) {

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

    override fun beforeInvoke(method: Method, args: Array<out Any>) {
        checkInit()
    }
}