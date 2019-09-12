package com.itangcent.common.function

import java.util.function.Function

interface Holder<T> {
    fun updateData(data: T?)

    fun getData(): T?

    fun updateData(updater: Function<T?, T?>): T?

    fun clear()

    companion object {
        fun <T> of(data: T): Holder<T> {
            return DefaultHolder(data)
        }

        fun <T> nil(): Holder<T> {
            return DefaultHolder(null)
        }
    }

    /**
     * Created by tangcent on 4/1/17.
     */
    open class DefaultHolder<T> @JvmOverloads constructor(data: T? = null) : Holder<T> {

        var holdData: T? = null
            private set

        init {
            this.holdData = data
        }

        override fun getData(): T? {
            return this.holdData
        }

        override fun updateData(data: T?) {
            this.holdData = data
        }

        override fun updateData(updater: Function<T?, T?>): T? {
            this.holdData = updater.apply(holdData)
            return this.holdData
        }

        override fun clear() {
            this.holdData = null
        }
    }
}

