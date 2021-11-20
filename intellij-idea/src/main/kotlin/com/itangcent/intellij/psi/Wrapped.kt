package com.itangcent.intellij.psi

import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.*
import java.util.concurrent.atomic.AtomicInteger

private val index = AtomicInteger()

interface Wrapped {
    fun get(): Any?
}

class WrappedValue(private var value: Any?) : KV<String, Any?>(), Wrapped {

    private val id = index.getAndIncrement()

    override fun get(): Any? {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WrappedValue

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

fun Any?.wrap(): WrappedValue {
    return WrappedValue(this)
}

@Deprecated(message = "use unwrap instead", replaceWith = ReplaceWith("T?.unwrap"))
@Suppress("UNCHECKED_CAST")
fun <T> T?.unwrapped(): T? {
    return this.unwrap()
}

@Suppress("UNCHECKED_CAST")
fun <T> T?.unwrap(): T? {
    return DefaultUnwrapper()(this) as T?
}

fun Any?.delay(): Delay {
    if (this is Delay) {
        return this
    }
    return Delay(this)
}

class Delay(private var raw: Any?) : Wrapped {

    private val id = index.getAndIncrement()

    override fun get(): Any? {
        return raw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Delay

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

fun Any?.upgrade(): Upgrade {
    if (this is Upgrade) {
        return this
    }
    return Upgrade(this)
}

class Upgrade(private var raw: Any?) : Wrapped {

    private val id = index.getAndIncrement()

    override fun get(): Any? {
        return null
    }

    fun core(): Any? {
        return raw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Upgrade

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

fun Any?.wrapped(deep: Int = 0): Boolean {
    when {
        this == null || deep > 10 -> {
            return false
        }
        this is Wrapped -> {
            return true
        }
        this is Collection<*> -> {
            return this.any { it.wrapped(deep + 1) }
        }
        this is Array<*> -> {
            return this.any { it.wrapped(deep + 1) }
        }
        this is Map<*, *> -> {
            return this.keys.any { it.wrapped(deep + 1) } || this.values.any { it.wrapped(deep + 1) }
        }
        else -> return false
    }
}

typealias Unwrapper = (Any?) -> Any?

class DefaultUnwrapper : Unwrapper {

    private val stack = ArrayDeque<Any>(5)
    private val unwrappedCache = SafeHashMap<Any, Any?>()

    override fun invoke(bean: Any?): Any? {
        val valueSetter = ResultSetter()
        bean.unwrap(0, null, null, valueSetter)
        return valueSetter.value()
    }

    @Suppress("UNCHECKED_CAST")
    fun Any?.unwrap(deep: Int = 0, parent: Map<Any?, Any?>? = null, key: Any? = null, valueSetter: ValueSetter) {
        if (this == null) {
            valueSetter(null)
            return
        }
        val count = stack.count { it == this }
        when {
            count < 2 -> {//can reentrant
                this.doUnwrappedWithStack(deep, parent, key, valueSetter)
            }
            this.wrapped(deep) -> {
                val cache = unwrappedCache[this]
                return if (cache == null) {
                    valueSetter(Any())
                } else {
                    valueSetter(cache.copy())
                }
            }
            else -> {
                valueSetter(this.copy())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Any.doUnwrappedWithStack(
        deep: Int,
        parent: Map<Any?, Any?>?,
        key: Any? = null,
        valueSetter: ValueSetter
    ) {
        stack.add(this)
        try {
            this.doUnwrapped(deep, parent, key) {
                unwrappedCache[this] = it
                valueSetter(it)
            }
        } finally {
            stack.removeLastOrNull()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun Any.doUnwrapped(
        deep: Int, parent: Map<Any?, Any?>?, key: Any? = null,
        valueSetter: ValueSetter
    ) {
        if (deep > 10) {
            valueSetter(this.copy())
            return
        }
        if (this is Wrapped) {
            val resultSetter = ResultSetter()
            if (SpiUtils.loadServices(UnwrapperSupporter::class)?.any { supporter ->
                    supporter.doUnwrapped(this, deep, parent, key, {
                        if (!resultSetter.setted()) {
                            it.unwrap(deep, parent, key, resultSetter)
                        }
                        resultSetter.value()
                    }, valueSetter)
                } == true) {
                return
            }
            this.get().unwrap(deep, parent, key, valueSetter)
            return
        }

        when {
            this.wrapped(deep) && this is Collection<*> -> {
                val copy = ArrayList<Any?>()
                valueSetter(copy)
                val subValueSetter = AddToCollection(copy)
                this.forEach { it.unwrap(deep + 1, parent, key, subValueSetter) }
            }
            this.wrapped(deep) && this is Array<*> -> {
                val copy = ArrayList<Any?>()
                valueSetter(copy)
                val subValueSetter = AddToCollection(copy)
                this.forEach { it.unwrap(deep + 1, parent, key, subValueSetter) }
            }
            this.wrapped(deep) && this is Map<*, *> -> {
                val copy = LinkedHashMap<Any?, Any?>()
                valueSetter(copy)

                this.forEach { entry ->
                    val keySetter = ResultSetter().also {
                        entry.key.unwrap(deep + 1, null, null, it)
                    }
                    if (keySetter.setted()) {
                        entry.value.unwrap(
                            deep + 1,
                            copy,
                            keySetter.value() as? String,
                            AddToMap(copy, keySetter.value())
                        )
                    }
                }
            }
            else -> {
                valueSetter(this.copy())
            }
        }
    }
}

typealias ValueSetter = (Any?) -> Unit

class ResultSetter : ValueSetter {
    private var setted = false
    private var value: Any? = null

    override fun invoke(value: Any?) {
        this.value = value
        this.setted = true
    }

    fun setted(): Boolean {
        return this.setted
    }

    fun value(): Any? {
        return this.value
    }
}

class AddToCollection(val collection: MutableCollection<*>) : ValueSetter {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: Any?) {
        (collection as MutableCollection<Any?>).add(value)
    }
}

class AddToMap(val map: MutableMap<*, *>, val key: Any?) : ValueSetter {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(value: Any?) {
        (map as MutableMap<Any?, Any?>)[key] = value
    }
}

interface UnwrapperSupporter {
    fun doUnwrapped(
        bean: Wrapped,
        deep: Int,
        parent: Map<Any?, Any?>?,
        key: Any?,
        unwrapper: Unwrapper,
        valueSetter: ValueSetter
    ): Boolean
}

/**
 * UnwrapperSupporter for [WrappedValue]
 */
class WrappedValueUnwrapperSupporter : UnwrapperSupporter {
    override fun doUnwrapped(
        bean: Wrapped,
        deep: Int,
        parent: Map<Any?, Any?>?,
        key: Any?,
        unwrapper: Unwrapper,
        valueSetter: ValueSetter
    ): Boolean {
        if (bean !is WrappedValue) {
            return false
        }
        fillExtras(bean, parent, key)
        valueSetter(unwrapper(bean.get()))
        return true
    }

    private fun fillExtras(value: WrappedValue, parent: Map<Any?, Any?>?, key: Any? = null) {
        if (parent == null || key == null) {
            return
        }
        value.forEach { k, v ->
            if ((k as? String)?.startsWith('@') == true) {
                val index = k.indexOf('@', 1)
                if (index == -1) {
                    parent.sub(k).mutable()[key as String] = v
                } else {
                    parent.sub(k.substring(0, index)).mutable()[key as String + "@" + k.substring(index + 1)] = v
                }
            }
        }
    }
}

/**
 * UnwrapperSupporter for [Upgrade]
 */
class UpgradeUnwrapperSupporter : UnwrapperSupporter {

    override fun doUnwrapped(
        bean: Wrapped,
        deep: Int,
        parent: Map<Any?, Any?>?,
        key: Any?,
        unwrapper: Unwrapper,
        valueSetter: ValueSetter
    ): Boolean {
        if (bean !is Upgrade) {
            return false
        }
        val core = unwrapper(bean.core())
        if (parent == null || core !is Map<*, *>) {
            valueSetter(core)
        } else {
            //merge to parent instead of call valueSetter
            parent.mutable().merge(core)
        }
        return true
    }
}
