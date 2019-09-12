package com.itangcent.common

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object Setup {

    private val setups: LinkedList<String> = LinkedList()

    fun setup(key: String, setup: Runnable) {

        if (setups.contains(key)) {
            return
        }
        synchronized(this) {
            if (!setups.contains(key)) {
                setups.add(key)
                setup.run()
            }
        }
    }

    fun setup(key: String, setup: () -> Unit) {
        if (setups.contains(key)) {
            return
        }
        synchronized(this) {
            if (!setups.contains(key)) {
                setups.add(key)
                setup()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun setup(setup: KClass<out SetupAble>) {
        val key = setup.qualifiedName!!
        if (setups.contains(key)) {
            return
        }
        val setupInstance = (setup as KClass<*>).createInstance() as SetupAble
        synchronized(this) {
            if (!setups.contains(key)) {
                setups.add(key)
                setupInstance.init()
            }
        }
    }
}