package com.itangcent.intellij.extend.rx

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.util.containers.Stack
import com.itangcent.intellij.util.changePropertyValue
import com.itangcent.intellij.util.getPropertyValue
import org.apache.commons.lang3.StringUtils
import org.junit.Assert
import java.awt.EventQueue
import java.util.*
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible

class AutoComputer {

    private val listeners = HashMap<AGetter<Any?>, () -> Unit>()

    private val passiveListeners = HashMap<ASetter<Any?>, () -> Unit>()

    private val wrapCache: Cache<Any?, Any?> = CacheBuilder
        .newBuilder()
        .build<Any?, Any?>()

    private var pool: (() -> Unit) -> Unit = { it() }

    /**
     * 赋值，并触发相应计算
     */
    public fun <T : Any?> value(property: KMutableProperty0<T>, value: T?) {
        property.safeSet(value)
        val getter: AGetter<Any?> = this.wrapGetter(property)
        call(getter)
    }

    /**
     * 赋值，并触发相应计算
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any?> value(setter: ASetter<T>, value: T?) {
        setter.set(value)
        if (setter is AGetter<*>) {
            call(setter as AGetter<Any?>)
        }
    }

    /**
     * 赋值，并触发相应计算
     */
    public fun <T> value(target: Any, property: String, value: T?) {
        value(wrapBeanProperty<T>(target, property), value)
    }

    //region 触发计算---------------------------------------------------------------
    internal fun call(getter: AGetter<Any?>) {
        val action = listeners[getter as AGetter<*>]
        var doAction = false
        try {
            if (action != null) {
                doAction = pushAction(action)
                if (doAction) {
                    //入栈成功，可执行
                    val actionStack = actionThreadLocal.get()
                    val rootThread = Thread.currentThread()
                    pool {
                        if (Thread.currentThread() != rootThread) {
                            actionThreadLocal.set(actionStack)
                            try {
                                action()
                            } finally {
                                actionThreadLocal.remove()
                            }
                        } else {
                            action()
                        }
                    }
                }
                //如果入栈失败，说明此Action在本次响应中已执行，则不再循环执行
            }

            //call relative
            listeners.keys
                .filter { isRelative(it, getter) }
                .forEach { listeners[it]?.invoke() }

            //call relative，重新计算子节点
            passiveListeners.keys
                .filter { isSon(it, getter) }
                .forEach { passiveListeners[it]?.invoke() }
        } finally {
            if (doAction) {
                popAction()
            }
        }

    }

    /**
     * 调用堆栈
     */
    private val actionThreadLocal: ThreadLocal<Stack<() -> Unit>> = ThreadLocal<Stack<() -> Unit>>()

    /**
     * 将一个计算Action推入堆栈，如果堆栈中已经有此Action，则推入失败
     */
    private fun pushAction(action: () -> Unit): Boolean {
        var actionStack = actionThreadLocal.get()
        return when {
            actionStack == null -> {
                actionStack = Stack(1)
                actionThreadLocal.set(actionStack)
                actionStack.push(action)
                true
            }
            actionStack.contains(action) -> false
            else -> {
                actionStack.push(action)
                true
            }
        }
    }

    /**
     * 弹出一个计算Action
     */
    private fun popAction() {
        val actionStack = actionThreadLocal.get()
        actionStack.pop()
        if (actionStack.isEmpty()) {
            actionThreadLocal.remove()
        }
    }
    //endregion 触发计算---------------------------------------------------------------

    private fun isRelative(getter: AGetter<*>, anotherGetter: AGetter<*>): Boolean {
        return when {
            getter == anotherGetter -> false
            getter !is HasProperty<*> -> false
            anotherGetter !is HasProperty<*> -> false
            getter.getProperty().target() != anotherGetter.getProperty().target() -> false
            getter.getProperty().name().startsWith(anotherGetter.getProperty().name() + ".") -> true
            anotherGetter.getProperty().name().startsWith(getter.getProperty().name() + ".") -> true
            else -> false
        };
    }

    /**
     * setter is son of getter
     */
    private fun isSon(setter: ASetter<*>, getter: AGetter<*>): Boolean {
        return when {
            setter == getter -> false
            setter !is HasProperty<*> -> false
            getter !is HasProperty<*> -> false
            setter.getProperty().target() != getter.getProperty().target() -> false
            setter.getProperty().name().startsWith(getter.getProperty().name() + ".") -> true
            else -> false
        };
    }

    /**
     * 开始绑定一个属性
     */
    public fun <T : Any?> bind(property: KMutableProperty0<T>): AutoBind0<T> {
        val wrapSetter = wrapSetter(property)
        return buildBind<T>(this, wrapSetter)
    }

    public fun bind(component: JTextComponent): AutoBind0<String?> {
        val wrapSetter = wrapJTextComponent(component)
        return buildBind<String?>(this, wrapSetter)
    }

    public fun bindEnable(component: JComponent): AutoBind0<Boolean> {
        val wrapSetter = wrapJComponentEnable(component)
        return buildBind<Boolean>(this, wrapSetter)
    }

    public fun bindIndex(component: JList<*>): AutoBind0<Int?> {
        val wrapSetter = wrapJListIndexComponent(component)
        return buildBind<Int?>(this, wrapSetter)
    }

    public fun <T> bind(target: Any, property: String): AutoBind0<T?> {
        val wrapSetter: ASetter<T?> = wrapBeanProperty<T>(target, property)
        return buildBind<T?>(this, wrapSetter)
    }

    public fun <T : Any> bind(target: Any, property: String, type: KClass<T>): AutoBind0<T> {
        val wrapSetter: ASetter<T?> = wrapBeanProperty<T>(target, property)
        return buildBind<T>(this, wrapSetter)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> wrapSetter(property: KMutableProperty0<T>): ASetter<T?> {
        return wrapCache.get(property) {
            KMutableProperty0Wrap<T>(property, parseProperty(property))
        } as ASetter<T?>
    }

    private fun wrapJComponentEnable(component: JComponent): JComponentEnableWrap {
        return wrapCache.get(component) {
            JComponentEnableWrap(component)
        } as JComponentEnableWrap
    }

    internal fun wrapJTextComponent(component: JTextComponent): JTextComponentWrap {
        return wrapCache.get(component) {
            JTextComponentWrap(component)
        } as JTextComponentWrap
    }

    internal fun wrapJListIndexComponent(component: JList<*>): JListComponentIndexWrap {
        return wrapCache.get(component) {
            JListComponentIndexWrap(component)
        } as JListComponentIndexWrap
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> wrapBeanProperty(target: Any, property: String): BeanPropertyWrap<T> {
        return wrapCache.get(Pair(target, property)) {
            val tinyProperty = StringUtils.removeStart(property, "this.")
            val lastDot = StringUtils.lastIndexOf(tinyProperty, ".")
            val targetExp = StringUtils.substring(tinyProperty, 0, lastDot)
            val propertyExp = StringUtils.substring(tinyProperty, lastDot + 1)

            val targetGetter: () -> Any?
            when {
                //空,直接取target
                StringUtils.isBlank(targetExp) -> targetGetter = { target }
                //一级，直接读属性
                targetExp.indexOf('.') == -1 -> targetGetter = { target.getPropertyValue(targetExp) }
                //多级,多次读属性,有一级为空，即返回null
                else -> {
                    val properties = targetExp.split('.')
                    targetGetter = {
                        var result: Any? = null
                        for (p in properties) {
                            result = target.getPropertyValue(p)
                            if (result == null)
                                break
                        }
                        result
                    }
                }
            }

            val propertySetter: (Any, T?) -> Unit = { any: Any, t: T? -> any.changePropertyValue(propertyExp, t) }

            val propertyGetter: ((Any) -> T?) = { any -> any.getPropertyValue(propertyExp) as T? }

            BeanPropertyWrap(targetGetter, propertySetter, propertyGetter, BeanProperty(target, tinyProperty))
        } as BeanPropertyWrap<T>
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> wrapGetter(property: KProperty0<T>): AGetter<T?> {
        if (property is KMutableProperty0) {
            return wrapCache.get(property) {
                KMutableProperty0Wrap<T>(property, parseProperty(property))
            } as AGetter<T?>
        } else {
            return wrapCache.get(property) {
                KProperty0Wrap<T>(property, parseProperty(property))
            } as AGetter<T?>
        }
    }

    fun parseProperty(property: KProperty0<*>): AProperty {
        return when (property) {
            is CallableReference -> BeanProperty(property.boundReceiver, property.name)
            else -> BeanProperty(null, property.name)
        }
    }

    fun addListeners(exp: () -> Unit, vararg properties: AGetter<Any?>) {
        for (property in properties) {
            mergeListeners(exp, property)
        }
    }

    fun addListeners(exp: () -> Unit, properties: List<AGetter<Any?>>) {
        for (property in properties) {
            mergeListeners(exp, property)
        }
    }

    fun mergeListeners(exp: () -> Unit, property: AGetter<Any?>) {
        val old = listeners.get(property)
        if (old == null) {
            property.onListen(this)
            listeners[property] = exp
        } else {
            listeners[property] = {
                old()
                exp()
            }
        }
    }

    /**
     * 一个属性只能被绑定一次，故不存在merge
     */
    fun addPassiveListeners(property: ASetter<Any?>, exp: () -> Unit) {
        val put = passiveListeners.put(property, exp)
        Assert.assertNull("a property should not bind twice!", put)
    }

    fun listenOn(pool: (() -> Unit) -> Unit): AutoComputer {
        this.pool = pool
        return this
    }

    class AutoBindData<T> {

        var computer: AutoComputer

        var property: ASetter<T?>

        var params: MutableList<AGetter<Any?>>

        var linkedParams: MutableList<AGetter<Any?>>? = null

        constructor(computer: AutoComputer, property: ASetter<T?>, params: MutableList<AGetter<Any?>>) {
            this.computer = computer
            this.property = property
            this.params = params
        }
    }

    abstract class AutoBind<T, E, C> {

        protected var core: AutoBindData<T>

        constructor(core: AutoBindData<T>) {
            this.core = core
        }

        @Suppress("UNCHECKED_CAST")
        fun link(param: KProperty0<Any>): C {
            val getter: AGetter<Any?> = core.computer.wrapGetter(param)
            if (core.linkedParams == null) {
                core.linkedParams = ArrayList()
                (core.linkedParams as MutableList<AGetter<Any?>>).add(getter)
            }
            return this as C
        }

        @Suppress("UNCHECKED_CAST")
        open fun eval(exp: E) {
//            val evalFun: () -> Unit = { pool { evalFun(exp) } }
            val evalFun: () -> Unit = evalFun(exp)
            val pooledEvalFun: () -> Unit = { pool(evalFun) }
            core.computer.addPassiveListeners(core.property as ASetter<Any?>, pooledEvalFun)
            core.computer.addListeners(pooledEvalFun, core.params)
            if (core.linkedParams != null) {
                for (kProperty in (core.linkedParams as List<AGetter<Any?>>)) {
                    core.computer.addListeners(pooledEvalFun, kProperty)
                }
            }
        }

        private var pool: (() -> Unit) -> Unit = { it() }

        @Suppress("UNCHECKED_CAST")
        fun listenOn(pool: (() -> Unit) -> Unit): C {
            this.pool = pool
            return this as C
        }

        protected abstract fun evalFun(exp: E): () -> Unit
    }

    class AutoBind0<T> : AutoBind<T, () -> T, AutoBind0<T>> {
        constructor(core: AutoBindData<T>) : super(core)

        @Suppress("UNCHECKED_CAST")
        fun <P> with(param: KProperty0<P>): AutoBind1<T, P> {
            val wrapGetter: AGetter<Any?> = core.computer.wrapGetter(param) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind1<T, P>(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P> with(target: Any, property: String): AutoBind1<T, P> {
            val wrapGetter: AGetter<Any?> = core.computer.wrapBeanProperty<P>(target, property) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind1<T, P>(core)
        }

        fun <P : Any> with(target: Any, property: String, type: KClass<P>): AutoBind1<T, P> {
            return with(target, property)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JTextComponent): AutoBind1<T, String?> {
            val wrapGetter: AGetter<String?> = core.computer.wrapJTextComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind1(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun withIndex(param: JList<*>): AutoBind1<T, Int?> {
            val wrapGetter: AGetter<Int?> = core.computer.wrapJListIndexComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind1(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P> withGetter(getter: AGetter<P>): AutoBind1<T, P> {
            this.core.params.add(getter as AGetter<Any?>)
            return AutoBind1<T, P>(core)
        }

        internal fun peakCore(): AutoBindData<T> {
            return core
        }

        override fun evalFun(exp: () -> T): () -> Unit {
            return { core.computer.value(core.property, exp()) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class AutoBind1<T, P1> : AutoBind<T, (P1) -> T, AutoBind1<T, P1>> {
        constructor(core: AutoBindData<T>) : super(core)

        fun <P> with(param: KProperty0<P>): AutoBind2<T, P1, P> {
            val wrapGetter: AGetter<Any?> = core.computer.wrapGetter(param) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind2<T, P1, P>(core)
        }

        fun <P> with(target: Any, property: String): AutoBind2<T, P1, P> {
            val wrapGetter: AGetter<Any?> = core.computer.wrapBeanProperty<P>(target, property) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind2<T, P1, P>(core)
        }

        fun <P : Any> with(target: Any, property: String, type: KClass<P>): AutoBind2<T, P1, P> {
            return with(target, property)
        }

        fun with(param: JTextComponent): AutoBind2<T, P1, String?> {
            val wrapGetter: AGetter<String?> = core.computer.wrapJTextComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind2<T, P1, String?>(core)
        }

        fun withIndex(param: JList<Any?>): AutoBind2<T, P1, Boolean?> {
            val wrapGetter: AGetter<Int?> = core.computer.wrapJListIndexComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind2<T, P1, Boolean?>(core)
        }

        override fun evalFun(exp: (P1) -> T): () -> Unit {
            return {
                core.computer.value(core.property, exp(core.params.get(0).get() as P1))
            }
        }
    }

    class AutoBind2<T, P1, P2> : AutoBind<T, (P1?, P2?) -> T?, AutoBind2<T, P1, P2>> {

        constructor(core: AutoBindData<T>) : super(core)

        @Suppress("UNCHECKED_CAST")
        fun <P> with(param: KProperty0<P>): AutoBind3<T, P1, P2, P> {
            val wrapGetter: AGetter<Any?> = core.computer.wrapGetter(param) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind3<T, P1, P2, P>(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P> with(target: Any, property: String): AutoBind3<T, P1, P2, P> {
            val wrapGetter: AGetter<Any?> = core.computer.wrapBeanProperty<P>(target, property) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind3<T, P1, P2, P>(core)
        }

        fun <P : Any> with(target: Any, property: String, type: KClass<P>): AutoBind3<T, P1, P2, P> {
            return with(target, property)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JTextComponent): AutoBind3<T, P1, P2, String?> {
            val wrapGetter: AGetter<String?> = core.computer.wrapJTextComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind3<T, P1, P2, String?>(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun withIndex(param: JList<Any?>): AutoBind3<T, P1, P2, Boolean?> {
            val wrapGetter: AGetter<Int?> = core.computer.wrapJListIndexComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind3<T, P1, P2, Boolean?>(core)
        }

        @Suppress("UNCHECKED_CAST")
        override fun evalFun(exp: (P1?, P2?) -> T?): () -> Unit {
            return {
                core.computer.value(
                    core.property,
                    exp(this.core.params.get(0).get() as P1, this.core.params.get(1).get() as P2)
                )
            }
        }
    }

    class AutoBind3<T, P1, P2, P3>(core: AutoBindData<T>) :
        AutoBind<T, (P1?, P2?, P3?) -> T?, AutoBind3<T, P1, P2, P3>>(core) {

        @Suppress("UNCHECKED_CAST")
        override fun evalFun(exp: (P1?, P2?, P3?) -> T?): () -> Unit {
            return {
                core.computer.value(
                    core.property, exp(
                        this.core.params.get(0).get() as P1,
                        this.core.params.get(1).get() as P2,
                        this.core.params.get(2).get() as P3
                    )
                )
            }
        }
    }

    companion object {
        fun <T> buildBind(computer: AutoComputer, property: ASetter<T?>): AutoBind0<T> {
            val data: AutoBindData<T> = AutoBindData(computer, property, ArrayList())
            return AutoBind0<T>(data)
        }
    }
}

//region 增强AutoBind0------------------------------------------------------------
// [consistent]:一对一直接绑定两个相同类型值
// [from]:直接取一个相同类型值
// [option]:转换为Optional对象求值
// [eval]:简化eval操作

fun <T> AutoComputer.AutoBind0<T>.from(param: KProperty0<T>) {
    this.with(param).eval()
}

fun <T> AutoComputer.AutoBind0<T>.from(target: Any, property: String) {
    this.with<T>(target, property).eval()
}

fun AutoComputer.AutoBind0<String?>.from(param: JTextComponent) {
    this.with(param).eval()
}

fun AutoComputer.AutoBind0<Int?>.from(param: JList<*>) {
    this.withIndex(param).eval()
}

fun <T, P> AutoComputer.AutoBind1<T, P?>.option(exp: (Optional<P>) -> T) {
    this.eval { p -> exp(Optional.ofNullable(p)) }
}

fun <T, X : T> AutoComputer.AutoBind1<T, X>.eval() {
    this.eval { r -> r }
}

@Suppress("UNCHECKED_CAST")
fun <T> AutoComputer.AutoBind0<T>.consistent(param: KProperty0<T>) {
    with(param).eval { r -> r }
    val core = peakCore()
    if (param is KMutableProperty0 && core.property is AGetter<*>) {
        core.computer.bind(param).withGetter(core.property as AGetter<T>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> AutoComputer.AutoBind0<T>.consistent(target: Any, property: String) {
    with<T>(target, property).eval { r -> r }
    val core = peakCore()
    if (core.property is AGetter<*>) {
        core.computer.bind<T>(target, property).withGetter(core.property as AGetter<T>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun AutoComputer.AutoBind0<String?>.consistent(param: JTextComponent) {
    val core = peakCore()
    val wrapGetter: AGetter<String?> = core.computer.wrapJTextComponent(param)
    withGetter(wrapGetter).eval { s -> s }
    if (core.property is AGetter<*>) {
        core.computer.bind(param).withGetter(core.property as AGetter<String?>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun AutoComputer.AutoBind0<Int?>.consistent(param: JList<*>) {
    val core = peakCore()
    val wrapGetter: AGetter<Int?> = core.computer.wrapJListIndexComponent(param)
    withGetter(wrapGetter).eval { index -> index }
    if (core.property is AGetter<*>) {
        core.computer.bindIndex(param).withGetter(core.property as AGetter<Int?>).eval { r -> r }
    }
}
//endregion 增强AutoBind0------------------------------------------------------------

fun <T> KMutableProperty<T>.safeSet(value: T?) {
    if (!this.isAccessible) {
        this.isAccessible = true
    }
    this.setter.call(value)
}

fun <T> KProperty<T>.safeGet(): T? {
    if (!this.isAccessible) {
        this.isAccessible = true
    }
    return this.getter.call()
}

fun <T> KProperty<T>.safeGet(target: Any): T? {
    if (!this.isAccessible) {
        this.isAccessible = true
    }
    return this.getter.call(target)
}

//region define interface-------------------------------------------------------
interface ASetter<T> {
    fun set(value: T?);
}

interface AGetter<T> {
    fun get(): T?;

    fun onListen(computer: AutoComputer) {

    }
}

interface AProperty {

    fun target(): Any?

    fun name(): String
}

interface HasProperty<T> {
    fun getProperty(): AProperty
}

class BeanProperty : AProperty {
    private var target: Any? = null
    private var name: String? = null

    constructor()

    constructor(target: Any?, name: String?) {
        this.target = target
        this.name = name
    }

    override fun target(): Any {
        return target!!
    }

    override fun name(): String {
        return name!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeanProperty

        if (target != other.target) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = target?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "BeanProperty(target=$target, name=$name)"
    }
}
//endregion define interface-------------------------------------------------------

//region wraps------------------------------------------------------------------

class KMutableProperty0Wrap<T> : ASetter<T>, AGetter<T>, HasProperty<T> {
    private val actualProperty: KMutableProperty0<T>
    private val aProperty: AProperty

    constructor(property: KMutableProperty0<T>, aProperty: AProperty) {
        this.actualProperty = property
        this.aProperty = aProperty
    }

    override fun set(value: T?) {
        this.actualProperty.safeSet(value)
    }

    override fun get(): T? {
        return this.actualProperty.safeGet()
    }

    override fun getProperty(): AProperty {
        return this.aProperty
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is HasProperty<*>) return false

        if (aProperty != other.getProperty()) return false

        return true
    }

    override fun hashCode(): Int {
        return aProperty.hashCode()
    }

    override fun toString(): String {
        return "KMutableProperty0Wrap(aProperty=$aProperty)"
    }
}

class KProperty0Wrap<T> : AGetter<T>, HasProperty<T> {

    private val actualProperty: KProperty0<T>
    private val aProperty: AProperty

    constructor(property: KProperty0<T>, aProperty: AProperty) {
        this.actualProperty = property
        this.aProperty = aProperty
    }

    override fun get(): T? {
        return this.actualProperty.safeGet()
    }

    override fun getProperty(): AProperty {
        return this.aProperty
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is HasProperty<*>) return false

        if (aProperty != other.getProperty()) return false

        return true
    }

    override fun hashCode(): Int {
        return aProperty.hashCode()
    }
}

class JTextComponentWrap : ASetter<String?>, AGetter<String?> {
    private val component: JTextComponent

    constructor(component: JTextComponent) {
        this.component = component
    }

    @Volatile
    private var cache: String? = null

    @Volatile
    private var manual: Boolean = false

    override fun set(value: String?) {
        cache = value
        EventQueue.invokeLater {
            manual = true
            this.component.text = value
            manual = false
        }
    }

    override fun get(): String? {
        return when {
            cache != null -> cache
            else -> this.component.text
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JTextComponentWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }

    private var hasListen = false;

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val jTextComponentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) {

                    if (manual) {
                        return
                    }
                    cache = null
                    computer.call(jTextComponentWrap)
                }

                override fun removeUpdate(e: DocumentEvent) {
                    if (manual) {
                        return
                    }
                    cache = null
                    computer.call(jTextComponentWrap)
                }

                override fun changedUpdate(e: DocumentEvent) {
                    cache = null
                    computer.call(jTextComponentWrap)
                }
            })
            hasListen = true
        }
    }
}

class JComponentEnableWrap : ASetter<Boolean?>, AGetter<Boolean?> {
    private val component: JComponent

    constructor(component: JComponent) {
        this.component = component
    }

    override fun set(value: Boolean?) {
        if (value != null) {
            if (this.component.isEnabled != value) {
                EventQueue.invokeLater { this.component.isEnabled = value }
            }
        }
    }

    override fun get(): Boolean? {
        return this.component.isEnabled
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JComponentEnableWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }

}

class JListComponentIndexWrap : ASetter<Int?>, AGetter<Int?> {
    private val component: JList<*>

    constructor(component: JList<*>) {
        this.component = component
    }

    @Volatile
    private var cache: Int? = null

    @Volatile
    private var manual: Boolean = false

    override fun set(value: Int?) {
        if (value != null) {
            cache = value
            EventQueue.invokeLater {
                manual = true
                this.component.selectedIndex = value
                manual = false
            }
        }
    }

    override fun get(): Int? {
        return this.component.selectedIndex
    }


    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val componentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.selectionModel.addListSelectionListener {
                if (!manual) {
                    computer.call(componentWrap)
                }
            }
            hasListen = true
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JListComponentIndexWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }
}

class BeanPropertyWrap<T> : ASetter<T?>, AGetter<T?>, HasProperty<T> {
    private val targetGetter: (() -> Any?)

    private val propertySetter: ((Any, T?) -> Unit)

    private val propertyGetter: ((Any) -> T?)

    private val aProperty: AProperty

    constructor(
        targetGetter: () -> Any?,
        propertySetter: (Any, T?) -> Unit,
        propertyGetter: (Any) -> T?,
        aProperty: AProperty
    ) {
        this.targetGetter = targetGetter
        this.propertySetter = propertySetter
        this.propertyGetter = propertyGetter
        this.aProperty = aProperty
    }

    override fun set(value: T?) {
        targetGetter()?.let { propertySetter(it, value) }
    }

    override fun get(): T? {
        return targetGetter()?.let { this.propertyGetter(it) }
    }

    override fun getProperty(): AProperty {
        return this.aProperty
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is HasProperty<*>) return false

        if (aProperty != other.getProperty()) return false

        return true
    }

    override fun hashCode(): Int {
        return aProperty.hashCode()
    }

    override fun toString(): String {
        return "BeanPropertyWrap(aProperty=$aProperty)"
    }
}
//endregion wraps------------------------------------------------------------------