package com.itangcent.intellij.extend.guice

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import java.lang.reflect.AccessibleObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class EnhancedInvocationHandler(
    private val invocationHandler: InvocationHandler,
    private val interceptors: List<MethodInterceptor>
) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<Any?>?): Any? {
        val methodInvocation = MethodInvocationImpl(invocationHandler, proxy!!, method!!, args ?: emptyArray())
        return ChainedMethodInvocation(interceptors, methodInvocation).proceed()
    }
}

class DelegateInvocationHandler(
    private val delegate: Any
) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<Any?>?): Any? {
        return method!!.invoke(delegate, *(args ?: emptyArray()))
    }
}

class MethodInvocationImpl(
    private val invocationHandler: InvocationHandler,
    private val target: Any?,
    private val method: Method,
    private val args: Array<Any?>
) : MethodInvocation {
    override fun proceed(): Any? = invocationHandler.invoke(target, method, args)

    override fun getThis(): Any? = target

    override fun getStaticPart(): AccessibleObject = method

    override fun getArguments(): Array<Any?> = args

    override fun getMethod(): Method = method
}

class ChainedMethodInvocation(
    private val interceptors: List<MethodInterceptor>,
    private val invocation: MethodInvocation
) : MethodInvocation by invocation {
    private var currentInterceptorIndex = 0

    override fun proceed(): Any? {
        return if (currentInterceptorIndex < interceptors.size) {
            val currentInterceptor = interceptors[currentInterceptorIndex]
            currentInterceptorIndex++
            currentInterceptor.invoke(this)
        } else {
            invocation.proceed()
        }
    }
}