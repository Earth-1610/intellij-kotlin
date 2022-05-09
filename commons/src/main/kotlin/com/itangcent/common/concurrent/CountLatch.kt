package com.itangcent.common.concurrent

import kotlin.jvm.Throws

interface CountLatch {

    /**
     * wait all acquired be released
     */
    @Throws(InterruptedException::class)
    fun waitFor()

    /**
     * wait all acquired be released
     */
    @Throws(InterruptedException::class)
    fun waitFor(msTimeout: Long): Boolean

    /**
     * acquire
     */
    fun down()

    /**
     * release
     */
    fun up()

    fun count(): Int
}