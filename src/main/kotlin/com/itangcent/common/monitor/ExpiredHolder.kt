package com.itangcent.common.monitor

import com.itangcent.common.function.Holder
import java.util.function.Consumer

interface ExpiredHolder<T> : Holder<T> {

    fun refresh(): ExpiredHolder<T>

    fun expire(time: Long): ExpiredHolder<T>

    fun onExpire(consumer: Consumer<T?>): ExpiredHolder<T>
}