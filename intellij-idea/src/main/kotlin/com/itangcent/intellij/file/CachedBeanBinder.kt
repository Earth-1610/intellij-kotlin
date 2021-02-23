package com.itangcent.intellij.file

import com.itangcent.intellij.context.ActionContext

/**
 * bind a file which content present a bean
 */
open class CachedBeanBinder<T : Any> : BeanBinder<T> {

    private var delegate: BeanBinder<T>

    @Volatile
    var cache: T? = null

    constructor(delegate: BeanBinder<T>) {
        this.delegate = delegate
    }

    override fun tryRead(): T? {
        if (cache == null) {
            cache = delegate.tryRead()
        }
        return cache
    }

    override fun read(): T {
        if (cache == null) {
            cache = delegate.read()
        }
        return cache as T
    }

    override fun save(t: T?) {
        cache = t
        val context = ActionContext.getContext()
        if (context == null) {
            delegate.save(t)
        } else {
            context.runAsync {
                delegate.save(t)
            }
        }
    }
}

fun <T : Any> BeanBinder<T>.lazy(): CachedBeanBinder<T> {
    return CachedBeanBinder(this)
}