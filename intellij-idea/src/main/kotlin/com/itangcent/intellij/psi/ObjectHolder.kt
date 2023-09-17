package com.itangcent.intellij.psi

import com.intellij.util.containers.Stack
import com.itangcent.common.utils.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val idx = AtomicLong()

interface ObjectHolder {

    val id: Long

    fun resolved(): Boolean

    fun resolve(context: Context?)

    fun onResolve(context: Context)

    fun getObject(): Any?

    fun circularEliminate(): Any?

    fun collectUnResolvedObjectHolders(action: (ObjectHolder) -> Unit)
}

fun Any?.asObjectHolder(): ObjectHolder {
    if (this == null) {
        return NULL_OBJECT_HOLDER
    }
    if (this is Array<*>) {
        return ArrayObjectHolder(this)
    }
    if (this is Collection<*>) {
        return CollectionObjectHolder((this as? MutableCollection<*>) ?: this.toMutableList())
    }
    if (this is Map<*, *>) {
        return MapObjectHolder(this.mutable())
    }
    if (this is ObjectHolder) {
        return this
    }
    return ResolvedObjectHolder(this)
}

fun ObjectHolder.notResolved(): Boolean {
    return !this.resolved()
}

fun ObjectHolder?.getOrResolve(): Any? {
    this ?: return null
    if (this.notResolved()) {
        val unResolvedObjectHolders = collectUnResolvedObjectHoldersAsList()
        for (unResolvedObjectHolder in unResolvedObjectHolders) {
            unResolvedObjectHolder.resolve(null)
        }
        if (this.notResolved()) {
            SimpleContext().with(this, null) {
                this.resolve(it)
                if (this.resolved()) {
                    this.onResolve(it)
                }
            }
        }
    }
    return this.getObject().copyUnsafe(4096)
}

private fun Any?.getOrCircularEliminate(): Any? {
    this ?: return null

    if (this !is ObjectHolder) {
        return this
    }

    if (this.resolved()) {
        return this.getObject()
    }

    return this.circularEliminate()
}

private class UnResolvedObjectHoldersAsListCollector(val unResolvedObjectHolders: LinkedList<ObjectHolder>) :
        (ObjectHolder) -> Unit {
    private val ids = HashSet<Long>()

    override fun invoke(objectHolder: ObjectHolder) {
        if (ids.add(objectHolder.id)) {
            unResolvedObjectHolders.addFirst(objectHolder)
            objectHolder.collectUnResolvedObjectHolders(this)
        }
    }
}

private fun ObjectHolder.collectUnResolvedObjectHoldersAsList(): LinkedList<ObjectHolder> {
    val unResolvedObjectHolders = LinkedList<ObjectHolder>()
    this.collectUnResolvedObjectHolders(UnResolvedObjectHoldersAsListCollector(unResolvedObjectHolders))
    return unResolvedObjectHolders
}

fun ObjectHolder.upgrade(): UpgradeObjectHolder {
    return if (this is UpgradeObjectHolder) {
        this
    } else {
        UpgradeObjectHolder(this)
    }
}

fun ObjectHolder.extend(): ExtendObjectHolder {
    return if (this is ExtendObjectHolder) {
        this
    } else {
        ExtendObjectHolder(this)
    }
}

interface Context {
    fun holders(): List<ObjectHolder>

    fun properties(): List<String?>

    fun contain(objectHolder: ObjectHolder): Boolean

    fun pushHolder(objectHolder: ObjectHolder, property: String?)

    fun pop()
}

class SimpleContext : Context {

    private val holders = Stack<ObjectHolder>()
    private val properties = Stack<String?>()

    override fun holders(): List<ObjectHolder> {
        return holders
    }

    override fun properties(): List<String?> {
        return properties
    }

    override fun contain(objectHolder: ObjectHolder): Boolean {
        val id = objectHolder.id
        return holders.any { it.id == id }
    }

    override fun pushHolder(objectHolder: ObjectHolder, property: String?) {
        holders.push(objectHolder)
        properties.push(property)
    }

    override fun pop() {
        holders.pop()
        properties.pop()
    }
}

fun Context.parent(): ObjectHolder? {
    return holders().lastOrNull()
}

fun Context.nearestMap(): ObjectHolder? {
    return holders().lastOrNull { it.getObject() is Map<*, *> }
}

fun Context.nearestProperty(): String? {
    return properties().lastOrNull { it != null }
}

fun Context.with(objectHolder: ObjectHolder, action: (Context) -> Unit) {
    this.pushHolder(objectHolder, null)
    try {
        action(this)
    } finally {
        this.pop()
    }
}

fun Context.with(objectHolder: ObjectHolder, property: String?, action: (Context) -> Unit) {
    this.pushHolder(objectHolder, property)
    try {
        action(this)
    } finally {
        this.pop()
    }
}

interface Extend {

    fun set(key: String, value: Any?): Extend

    fun extends(): MutableMap<String, Any?>?
}

class ResolvedObjectHolder(private val obj: Any?) : ObjectHolder {
    override val id = idx.getAndIncrement()

    override fun resolved(): Boolean {
        return true
    }

    override fun resolve(context: Context?) {
        //NOP
    }

    override fun onResolve(context: Context) {
        //NOP
    }

    override fun getObject(): Any? {
        return this.obj
    }

    override fun circularEliminate(): Any? {
        return this.obj
    }

    override fun collectUnResolvedObjectHolders(action: (ObjectHolder) -> Unit) {
        //NOP
    }
}

val NULL_OBJECT_HOLDER = ResolvedObjectHolder(null)

abstract class UnstableObjectHolder<T>(protected val rawObj: T) : ObjectHolder {

    override val id = idx.getAndIncrement()

    /**
     * resolveState is stored in the low-order 3 bits
     * other flags is stored in the higher bits
     */
    protected var resolveFlag: Int = UNRESOLVED

    protected var resolvedObj: T? = null

    override fun resolved(): Boolean {
        return isState(RESOLVED)
    }

    override fun onResolve(context: Context) {
        //NOP
    }

    protected fun Any?.getObject(context: Context?, target: (Any?) -> Unit) {
        this.getObject(context, null, target) { }
    }

    protected fun Any?.getObject(context: Context?, key: String?, target: (Any?) -> Unit, onIgnore: () -> Unit) {
        var obj: Any? = this
        if (this is ObjectHolder) {
            when {
                context == null -> {
                    obj = this
                    setState(UNRESOLVED)
                }

                context.contain(this) -> {
                    context.with(this, key) {
                        this.onResolve(context)
                        obj = this.circularEliminate()
                    }
                }

                this.resolved() -> {
                    context.with(this, key) {
                        this.onResolve(context)
                        obj = this.getObject()
                    }
                }

                else -> {
                    context.with(this, key) {
                        this.resolve(it)
                        this.onResolve(it)
                        obj = this.getObject()
                    }
                }
            }
        }
        if (obj === IGNORE) {
            onIgnore()
            return
        } else {
            target(obj)
        }
    }

    override fun resolve(context: Context?) {
        if (!isState(UNRESOLVED)) {
            return
        }
        setState(RESOLVING)
        val copiedObject = createCopied()
        this.resolvedObj = copiedObject
        resolveToCopiedObject(context, copiedObject)
        if (isState(RESOLVING)) {
            setState(RESOLVED)
        }
    }


    protected abstract fun createCopied(): T

    protected abstract fun resolveToCopiedObject(context: Context?, t: T)

    override fun getObject(): Any? {
        return this.resolvedObj ?: this.rawObj
    }

    //region state & flag

    private fun isState(state: Int) = this.resolveFlag and RESOLVE_STATE_BITS == state

    private fun setState(state: Int) {
        this.resolveFlag = state xor (this.resolveFlag and BEYOND_STATE_BITS)
    }

    protected fun hasFlag(flag: Int): Boolean = this.resolveFlag and flag != 0

    fun setFlg(flag: Int) {
        this.resolveFlag = this.resolveFlag xor flag
    }

    fun removeFlag(flag: Int) {
        this.resolveFlag = this.resolveFlag and flag.inv()
    }

    //endregion

    companion object {

        //resolveState is stored in the low-order 3 bits
        const val RESOLVE_STATE_BITS = 0b11;

        const val UNRESOLVED = 0;
        const val RESOLVING = 1;
        const val RESOLVED = 2;

        //other flags is stored in the higher bits
        const val BEYOND_STATE_BITS = RESOLVE_STATE_BITS.inv();

        const val CIRCULAR_ELIMINATE = 0b100;
    }
}

class ArrayObjectHolder(obj: Array<*>) : UnstableObjectHolder<Array<*>>(obj) {

    override fun circularEliminate(): Any? {
        if (hasFlag(CIRCULAR_ELIMINATE)) {
            return emptyArray<Any?>()
        }
        setFlg(CIRCULAR_ELIMINATE)
        val copy = ArrayList<Any?>()
        rawObj.forEach {
            copy.add(it.getOrCircularEliminate())
        }
        removeFlag(CIRCULAR_ELIMINATE)
        return copy.toArray()
    }

    override fun collectUnResolvedObjectHolders(action: (ObjectHolder) -> Unit) {
        rawObj.forEach { ele ->
            (ele as? ObjectHolder)?.takeIf { it.notResolved() }
                ?.let(action)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun createCopied(): Array<*> {
        return this.rawObj.copyOf() as Array<Any?>
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolveToCopiedObject(context: Context?, t: Array<*>) {
        t as Array<Any?>
        rawObj.forEachIndexed { index, ele ->
            ele.getObject(context) {
                t[index] = it
            }
        }
    }
}

class CollectionObjectHolder(obj: MutableCollection<*>) : UnstableObjectHolder<MutableCollection<*>>(obj) {

    override fun circularEliminate(): Any {
        if (hasFlag(CIRCULAR_ELIMINATE)) {
            return emptyArray<Any?>()
        }
        setFlg(CIRCULAR_ELIMINATE)
        val copy = ArrayList<Any?>()
        rawObj.forEach {
            copy.add(it.getOrCircularEliminate())
        }
        removeFlag(CIRCULAR_ELIMINATE)
        return copy
    }

    override fun collectUnResolvedObjectHolders(action: (ObjectHolder) -> Unit) {
        rawObj.forEach { ele ->
            (ele as? ObjectHolder)?.takeIf { it.notResolved() }
                ?.let(action)
        }
    }

    override fun createCopied(): MutableCollection<*> {
        return ArrayList<Any?>()
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolveToCopiedObject(context: Context?, t: MutableCollection<*>) {
        t as MutableCollection<Any?>
        rawObj.forEach { ele ->
            ele.getObject(context) {
                t.add(it)
            }
        }
    }
}

class MapObjectHolder(obj: MutableMap<*, *>) : UnstableObjectHolder<MutableMap<*, *>>(obj) {

    override fun circularEliminate(): Any {
        if (hasFlag(CIRCULAR_ELIMINATE)) {
            return emptyMap<Any?, Any?>()
        }
        setFlg(CIRCULAR_ELIMINATE)
        val copy = LinkedHashMap<Any?, Any?>()
        this.rawObj.entries.forEach {
            //keep the extends
            if (it.key.isExtendKey()) {
                copy[it.key] = it.value
                return@forEach
            }

            copy[it.key.getOrCircularEliminate()] = it.value.getOrCircularEliminate()
        }
        removeFlag(CIRCULAR_ELIMINATE)
        return copy
    }

    override fun collectUnResolvedObjectHolders(action: (ObjectHolder) -> Unit) {
        rawObj.forEach { entry ->
            (entry.key as? ObjectHolder)?.takeIf { it.notResolved() }
                ?.let(action)
            (entry.value as? ObjectHolder)?.takeIf { it.notResolved() }
                ?.let(action)
        }
    }

    private fun Any?.isExtendKey(): Boolean {
        return this is String && this.startsWith("@")
    }

    override fun createCopied(): MutableMap<*, *> {
        return LinkedHashMap<Any?, Any?>()
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolveToCopiedObject(context: Context?, t: MutableMap<*, *>) {
        t as MutableMap<Any?, Any?>
        for ((k, v) in this.rawObj.entries) {
            //keep the extends
            if (k.isExtendKey()) {
                t.merge(k, v)
                continue
            }
            k.getObject(context) { key ->
                v.getObject(context, key?.toString(), { value ->
                    t.merge(key, value)
                }) {}
            }
        }
    }
}

class UpgradeObjectHolder(private val objHolder: ObjectHolder) : ObjectHolder by objHolder {

    override fun onResolve(context: Context) {
        objHolder.onResolve(context)
        val obj = objHolder.getObject()
        if (obj !is Map<*, *>) {
            LOG.warn("failed upgrade object:$obj")
            return
        }
        val target = context.nearestMap()?.getObject() as? MutableMap<*, *> ?: return
        target.merge(obj)
    }

    override fun getObject(): Any {
        return IGNORE
    }
}

class ExtendObjectHolder(private val objHolder: ObjectHolder) : ObjectHolder by objHolder,
    Extend {

    private var extend: MutableMap<String, Any?>? = null

    override fun set(key: String, value: Any?): ExtendObjectHolder {
        if (extend == null) {
            extend = linkedMapOf()
        }
        extend!![key] = value
        return this
    }

    override fun extends(): MutableMap<String, Any?>? {
        return extend
    }

    @Suppress("UNCHECKED_CAST")
    override fun onResolve(context: Context) {
        objHolder.onResolve(context)
        val target = context.nearestMap()?.getObject() as? Map<*, *> ?: return
        val property: String = context.nearestProperty() ?: return
        this.extend?.forEach { (k, v) ->
            if (k.startsWith('@')) {
                val index = k.indexOf('@', 1)
                if (index == -1) {
                    target.sub(k).mutable().merge(property, v)
                } else {
                    val key = k.substring(0, index)
                    val subKey = property + "@" + k.substring(index + 1)
                    target.sub(key).mutable().merge(subKey, v)
                }
            } else {
                (target as? MutableMap<Any?, Any?>)?.merge(k, v)
            }
        }
    }
}

//background idea log
private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ObjectHolder::class.java)

private val IGNORE = Any()