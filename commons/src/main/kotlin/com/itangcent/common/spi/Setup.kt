package com.itangcent.common.spi

import com.itangcent.common.logger.ILogger
import com.itangcent.common.logger.traceError
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object Setup {

    private val setups: HashSet<String> = HashSet()

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
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)
        logger?.debug("try setup key:$key")
        if (setups.contains(key)) {
            logger?.debug("key:$key has already setup")
            return
        }
        synchronized(this) {
            if (setups.add(key)) {
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

    fun load() {
        load(SetupAble::class.java.classLoader)
    }

    fun load(classLoader: ClassLoader) {
        val logger: ILogger? = SpiUtils.loadService(ILogger::class)
        val setupAbles = ServiceLoader.load(
            SetupAble::class.java, classLoader
        )
        for (setupAble in setupAbles) {
            logger?.debug("try setup:$setupAble")
            try {
                setup(setupAble)
                logger?.debug("setup:$setupAble success")
            } catch (e: Throwable) {
                logger?.traceError("setup:$setupAble failed", e)
            }
        }
    }

    private fun setup(setup: SetupAble) {
        setup(setup::class.qualifiedName ?: setup.toString()) {
            setup.init()
        }
    }
}