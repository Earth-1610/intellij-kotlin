package com.itangcent.common.monitor

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Function

class GuavaExpiredHolder<T> : ExpiredHolder<T> {

    companion object {
        const val split: String = "_"
        const val defaultKey: String = "default_key"
        val nullObject = java.lang.Object()
    }

    private var expire: Long? = null

    private var expireHandle: AtomicReference<Consumer<T?>> = AtomicReference()
    private var cache: AtomicReference<Cache<String, Any>> =
        AtomicReference()
    private val index: AtomicInteger = AtomicInteger()

    override fun refresh(): ExpiredHolder<T> {
        val index = this.index.get()
        val data = safeGetCache().getIfPresent("$defaultKey$split$index")
        if (data != null && data != nullObject) {
            cache.get().put("$defaultKey$split$index", data)
        }
        return this;
    }

    override fun expire(time: Long): ExpiredHolder<T> {
        if (this.expire != time) {
            this.expire = time
            update()
        }
        return this;
    }

    override fun onExpire(consumer: Consumer<T?>): ExpiredHolder<T> {
        expireHandle.set(consumer)
        return this;
    }

    override fun updateData(data: T?) {

        val index = this.index.incrementAndGet()
        safeGetCache().put("$defaultKey$split$index", notNull(data))
    }

    @Suppress("UNCHECKED_CAST")
    override fun getData(): T? {
        return nullAble(safeGetCache().getIfPresent("$defaultKey$split${index.get()}")) as T?
    }

    override fun updateData(updater: Function<T?, T?>): T? {
        val newData = updater.apply(getData())
        updateData(newData)
        return newData
    }

    override fun clear() {
        this.cache.set(null)
    }

    @Suppress("UNCHECKED_CAST")
    fun update() {
        val builder = CacheBuilder.newBuilder()
        if (expire != null) {
            builder.expireAfterWrite(expire!!, TimeUnit.MILLISECONDS)
        }
        builder.removalListener(RemovalListener { notification ->
            expireHandle.get()?.accept(notification.value as T?)
        })
        cache.set(builder.build<String, Any>())
    }

    private fun safeGetCache(): Cache<String, Any> {
        val cache = this.cache.get()
        if (cache == null) {
            update()
        }
        return this.cache.get()
    }

    private fun notNull(obj: Any?): Any {
        when (obj) {
            null -> return nullObject
            else -> return obj
        }
    }

    private fun nullAble(obj: Any?): Any? {
        when (obj) {
            null -> return null
            nullObject -> return null
            else -> return obj
        }
    }
}
