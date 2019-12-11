package com.itangcent.common

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object Setup {

    private val setups: LinkedHashSet<String> = LinkedHashSet()

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

    private var loadAble = true
    fun load() {
        synchronized(this) {
            if (loadAble) {
                loadAble = false
                val setupAbles = ServiceLoader.load(
                    SetupAble::class.java,
                    SetupAble::class.java.classLoader
                )
                for (setupAble in setupAbles) {
                    setup(setupAble)
                }
            }
        }
    }

    private fun setup(setup: SetupAble) {
        setup(setup::class.qualifiedName ?: setup.toString()) {
            setup.init()
        }
    }
}