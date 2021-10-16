package com.itangcent.common.spi

import com.itangcent.common.utils.notNullOrEmpty
import com.itangcent.common.utils.privileged
import java.lang.reflect.*
import java.util.*
import kotlin.reflect.KClass

open class ProxyBean(private val delegates: Array<Any>) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        return invoke(method, args ?: emptyArray())
    }

    fun invoke(method: Method, args: Array<out Any>): Any? {
        beforeInvoke(method, args)
        var availableRet: Any? = null
        var throwables: MutableList<Throwable>? = null
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
                if (throwables == null) {
                    throwables = LinkedList()
                }
                throwables.add(e)
            }
        }
        var setRet = false
        afterInvoke(method, args, availableRet, throwables) {
            availableRet = it
            setRet = true
        }
        if (setRet) {
            return availableRet
        }
        availableRet?.let { return it }
        throwables?.let { throw it.first() }
        return null
    }

    open fun beforeInvoke(method: Method, args: Array<out Any>) {

    }

    open fun afterInvoke(
        method: Method,
        args: Array<out Any>,
        ret: Any?,
        throwables: List<Throwable>?,
        resultSetter: (Any?) -> Unit
    ) {
    }

    private fun Throwable.isNotImplemented(): Boolean {
        return when (this) {
            is UndeclaredThrowableException -> this.undeclaredThrowable.isNotImplemented()
            is InvocationTargetException -> this.targetException.isNotImplemented()
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

open class SafeProxyBean(private val delegates: Array<Any>) : ProxyBean(delegates) {
    override fun afterInvoke(
        method: Method,
        args: Array<out Any>,
        ret: Any?,
        throwables: List<Throwable>?,
        resultSetter: (Any?) -> Unit
    ) {
        //return null instead of throw an exception
        if (ret == null && throwables.notNullOrEmpty()) {
            resultSetter(null)
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <S : Any> ProxyBean.createProxy(cls: KClass<S>): S {
    return Proxy.newProxyInstance(cls.java.classLoader, arrayOf(cls.java), this) as S
}