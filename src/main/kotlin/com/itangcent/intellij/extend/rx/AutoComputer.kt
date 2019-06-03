package com.itangcent.intellij.extend.rx

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.util.containers.Stack
import com.itangcent.common.utils.IDUtils
import com.itangcent.intellij.extend.rx.AutoComputerUtils.mergeFilter
import com.itangcent.intellij.util.changePropertyValue
import com.itangcent.intellij.util.getPropertyValue
import org.apache.commons.lang3.StringUtils
import java.awt.Component
import java.awt.EventQueue
import java.util.*
import javax.swing.*
import javax.swing.event.*
import javax.swing.text.JTextComponent
import kotlin.collections.ArrayList
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible

class AutoComputer {

    private val throttleHelper: ThrottleHelper = ThrottleHelper()

    private val listeners = HashMap<AGetter<Any?>, () -> Unit>()

    private val passiveListeners = HashMap<ASetter<Any?>, () -> Unit>()

    private val wrapCache: Cache<Any?, Any?> = CacheBuilder
        .newBuilder()
        .build()

    private var pool: (() -> Unit) -> Unit = { it() }

    /**
     * set value,and compute
     */
    public fun <T> value(property: KMutableProperty0<T>, value: T) {
        property.safeSet(value)
        val getter: AGetter<Any?> = this.wrapGetter(property)
        call(getter)
    }

    /**
     * set value,and compute
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any?> value(setter: ASetter<T>, value: T?) {
        setter.set(value)
        if (setter is AGetter<*>) {
            call(setter as AGetter<Any?>)
        }
    }

    /**
     * set value,and compute
     */
    public fun <T> value(target: Any, property: String, value: T?) {
        value(wrapBeanProperty<T>(target, property), value)
    }

    //region try compute---------------------------------------------------------------
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

    private val actionThreadLocal: ThreadLocal<Stack<() -> Unit>> = ThreadLocal<Stack<() -> Unit>>()

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

    private fun popAction() {
        val actionStack = actionThreadLocal.get()
        actionStack.pop()
        if (actionStack.isEmpty()) {
            actionThreadLocal.remove()
        }
    }
    //endregion try compute---------------------------------------------------------------

    private fun isRelative(getter: AGetter<*>, anotherGetter: AGetter<*>): Boolean {
        return when {
            getter == anotherGetter -> false
            getter !is HasProperty<*> -> false
            anotherGetter !is HasProperty<*> -> false
            getter.getProperty().target() != anotherGetter.getProperty().target() -> false
            getter.getProperty().name().startsWith(anotherGetter.getProperty().name() + ".") -> true
            anotherGetter.getProperty().name().startsWith(getter.getProperty().name() + ".") -> true
            else -> false
        }
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
        }
    }

    fun <T : Any?> bind(property: KMutableProperty0<T>): AutoBind0<T> {
        val wrapSetter = wrapSetter(property)
        return buildBind<T>(this, wrapSetter)
    }

    fun bind(component: JTextComponent): AutoBind0<String?> {
        val wrapSetter = wrapJTextComponent(component)
        return buildBind(this, wrapSetter)
    }

    fun bindText(component: AbstractButton): AutoBind0<String?> {
        val wrapSetter = wrapJButtonTextComponent(component)
        return buildBind(this, wrapSetter)
    }

    fun bind(component: JCheckBox): AutoBind0<Boolean?> {
        val wrapSetter = wrapJCheckBoxComponentWrap(component)
        return buildBind(this, wrapSetter)
    }

    fun bind(component: JLabel): AutoBind0<String?> {
        val wrapSetter = wrapJLabel(component)
        return buildBind(this, wrapSetter)
    }

    fun bindEnable(component: JComponent): AutoBind0<Boolean> {
        val wrapSetter = wrapComponentEnable(component)
        return buildBind(this, wrapSetter)
    }

    fun bindVisible(component: JComponent): AutoBind0<Boolean> {
        val wrapSetter = wrapComponentVisible(component)
        return buildBind(this, wrapSetter)
    }

    fun bindName(component: JComponent): AutoBind0<String> {
        val wrapSetter = wrapComponentName(component)
        return buildBind(this, wrapSetter)
    }

    fun bindIndex(component: JList<*>): AutoBind0<Int?> {
        val wrapSetter = wrapJListIndexComponent(component)
        return buildBind(this, wrapSetter)
    }

    fun bind(component: JList<*>): AutoBind0<List<*>?> {
        val wrapSetter = wrapJListComponent(component)
        return buildBind(this, wrapSetter)
    }

    fun bindIndex(component: JComboBox<*>): AutoBind0<Int?> {
        val wrapSetter = wrapJComboBoxIndexComponent(component)
        return buildBind(this, wrapSetter)
    }

    fun <T> bind(component: JComboBox<T>): AutoBind0<T?> {
        val wrapSetter = wrapJComboBoxComponent(component)
        return buildBind(this, wrapSetter)
    }

    fun <T> bind(target: Any, property: String): AutoBind0<T?> {
        val wrapSetter: ASetter<T?> = wrapBeanProperty<T>(target, property)
        return buildBind<T?>(this, wrapSetter)
    }

    fun <T : Any> bind(target: Any, property: String, type: KClass<T>): AutoBind0<T> {
        val wrapSetter: ASetter<T?> = wrapBeanProperty<T>(target, property)
        return buildBind(this, wrapSetter)
    }

    //region ***************listen***************-------------------------------

    fun <T : Any?> listen(property: KMutableProperty0<T>): ListenAble<T> {
        val wrapGetter = wrapGetter(property)
        return ListenAble(this, wrapGetter)
    }

    fun listen(component: JTextComponent): ListenAble<String> {
        val wrapGetter = wrapJTextComponent(component)
        return ListenAble(this, wrapGetter)
    }

    fun listenText(component: AbstractButton): ListenAble<String> {
        val wrapGetter = wrapJButtonTextComponent(component)
        return ListenAble(this, wrapGetter)
    }

    fun listen(component: JCheckBox): ListenAble<Boolean> {
        val wrapGetter = wrapJCheckBoxComponentWrap(component)
        return ListenAble(this, wrapGetter)
    }

    fun listen(component: JLabel): ListenAble<String> {
        val wrapGetter = wrapJLabel(component)
        return ListenAble(this, wrapGetter)
    }

    fun listenEnable(component: JComponent): ListenAble<Boolean> {
        val wrapGetter = wrapComponentEnable(component)
        return ListenAble(this, wrapGetter)
    }

    fun listenVisible(component: JComponent): ListenAble<Boolean> {
        val wrapGetter = wrapComponentVisible(component)
        return ListenAble(this, wrapGetter)
    }

    fun listenName(component: JComponent): ListenAble<String> {
        val wrapGetter = wrapComponentName(component)
        return ListenAble(this, wrapGetter)
    }

    fun listenIndex(component: JList<*>): ListenAble<Int> {
        val wrapGetter = wrapJListIndexComponent(component)
        return ListenAble(this, wrapGetter)
    }

    fun listen(component: JList<*>): ListenAble<List<*>> {
        val wrapGetter = wrapJListComponent(component)
        return ListenAble(this, wrapGetter)
    }

    fun listenIndex(component: JComboBox<*>, action: (Int?) -> Unit): ListenAble<Int> {
        val wrapGetter = wrapJComboBoxIndexComponent(component)
        return ListenAble(this, wrapGetter)
    }

    fun <T> listen(component: JComboBox<T>): ListenAble<T> {
        val wrapGetter = wrapJComboBoxComponent(component)
        return ListenAble(this, wrapGetter)
    }

    fun <T> listen(target: Any, property: String): ListenAble<T> {
        val wrapGetter: AGetter<T?> = wrapBeanProperty(target, property)
        return ListenAble(this, wrapGetter)
    }

    fun <T : Any> listen(
        target: Any,
        property: String,
        type: KClass<T>
    ): ListenAble<T> {
        val wrapGetter: AGetter<T?> = wrapBeanProperty(target, property)
        return ListenAble(this, wrapGetter)
    }

    internal fun <T> singleListen(
        wrapGetter: AGetter<T?>,
        action: (T?) -> Unit
    ) {
        addListeners({
            action(wrapGetter.get())
        }, wrapGetter as AGetter<Any?>)
    }

    //endregion ***************listen***************-------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> wrapSetter(property: KMutableProperty0<T>): ASetter<T?> {
        return wrapCache.get(property) {
            KMutableProperty0Wrap<T>(property, parseProperty(property))
        } as ASetter<T?>
    }

    private fun wrapComponentEnable(component: Component): ComponentEnableWrap {
        return wrapCache.get(component to "enable") {
            ComponentEnableWrap(component)
        } as ComponentEnableWrap
    }

    private fun wrapComponentVisible(component: Component): ComponentVisibleWrap {
        return wrapCache.get(component to "visible") {
            ComponentVisibleWrap(component)
        } as ComponentVisibleWrap
    }

    private fun wrapComponentName(component: Component): ComponentNameWrap {
        return wrapCache.get(component to "name") {
            ComponentNameWrap(component)
        } as ComponentNameWrap
    }

    internal fun wrapJTextComponent(component: JTextComponent): JTextComponentWrap {
        return wrapCache.get(component) {
            JTextComponentWrap(component)
        } as JTextComponentWrap
    }

    internal fun wrapJButtonTextComponent(component: AbstractButton): JButtonTextComponentWrap {
        return wrapCache.get(component) {
            JButtonTextComponentWrap(component)
        } as JButtonTextComponentWrap
    }

    internal fun wrapJCheckBoxComponentWrap(component: JCheckBox): JCheckBoxComponentWrap {
        return wrapCache.get(component) {
            JCheckBoxComponentWrap(component)
        } as JCheckBoxComponentWrap
    }

    internal fun wrapJLabel(component: JLabel): JLabelWrap {
        return wrapCache.get(component) {
            JLabelWrap(component)
        } as JLabelWrap
    }

    internal fun wrapJListIndexComponent(component: JList<*>): JListComponentIndexWrap {
        return wrapCache.get(component to "index") {
            JListComponentIndexWrap(component)
        } as JListComponentIndexWrap
    }

    internal fun wrapJListComponent(component: JList<*>): JListComponentWrap {
        return wrapCache.get(component) {
            JListComponentWrap(component)
        } as JListComponentWrap
    }

    internal fun wrapJComboBoxIndexComponent(component: JComboBox<*>): JComboBoxComponentIndexWrap {
        return wrapCache.get(component to "index") {
            JComboBoxComponentIndexWrap(component)
        } as JComboBoxComponentIndexWrap
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> wrapJComboBoxComponent(component: JComboBox<T>): JComboBoxComponentWrap<T> {
        return wrapCache.get(component) {
            JComboBoxComponentWrap(component)
        } as JComboBoxComponentWrap<T>
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
                KMutableProperty0Wrap(property, parseProperty(property))
            } as AGetter<T?>
        } else {
            return wrapCache.get(property) {
                KProperty0Wrap(property, parseProperty(property))
            } as AGetter<T?>
        }
    }

    private fun parseProperty(property: KProperty0<*>): AProperty {
        return when (property) {
            is CallableReference -> BeanProperty(property.boundReceiver, property.name)
            else -> BeanProperty(null, property.name)
        }
    }

    private fun addListeners(exp: () -> Unit, vararg properties: AGetter<Any?>) {
        for (property in properties) {
            mergeListeners(exp, property)
        }
    }

    private fun addListeners(exp: () -> Unit, properties: List<AGetter<Any?>>) {
        for (property in properties) {
            mergeListeners(exp, property)
        }
    }

    private fun mergeListeners(exp: () -> Unit, property: AGetter<Any?>) {
        val old = listeners[property]
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

    private fun addPassiveListeners(property: ASetter<Any?>, exp: () -> Unit) {
        val old = passiveListeners[property]
        if (old == null) {
            passiveListeners[property] = exp
        } else {
            passiveListeners[property] = {
                old()
                exp()
            }
        }
//        val put = passiveListeners.put(property, exp)
//        Assert.assertNull("a property should not bind twice!", put)
    }

    fun listenOn(pool: (() -> Unit) -> Unit): AutoComputer {
        this.pool = pool
        return this
    }

    class AutoBindData<T> {

        var computer: AutoComputer

        var filter: (Filter)? = null

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
                core.linkedParams!!.add(getter)
            }
            return this as C
        }

        @Suppress("UNCHECKED_CAST")
        open fun eval(exp: E) {
//            val evalFun: () -> Unit = { pool { evalFun(exp) } }
            var evalFun: () -> Unit = evalFun(exp)
            val wrapPool = pool
            if (wrapPool != null) {
                val wrapFun = evalFun
                evalFun = { wrapPool(wrapFun) }
            }
            val wrapFilter = core.filter
            if (wrapFilter != null) {
                val wrapFun = evalFun
                evalFun = {
                    if (wrapFilter()) {
                        wrapFun()
                    }
                }
            }

            core.computer.addPassiveListeners(core.property as ASetter<Any?>, evalFun)
            core.computer.addListeners(evalFun, core.params)
            if (core.linkedParams != null) {
                for (kProperty in (core.linkedParams as List<AGetter<Any?>>)) {
                    core.computer.addListeners(evalFun, kProperty)
                }
            }
        }

        fun throttle(cd: Long): C {
            val throttleFilter = { computer().throttleHelper.acquire(core.property, cd) }
            val filter = this.core.filter
            if (filter == null) {
                this.core.filter = throttleFilter
            } else {
                this.core.filter = {
                    filter() && throttleFilter()
                }
            }
            return this as C
        }

        private var pool: ((() -> Unit) -> Unit)? = null

        @Suppress("UNCHECKED_CAST")
        fun listenOn(pool: (() -> Unit) -> Unit): C {
            this.pool = pool
            return this as C
        }

        protected fun computer(): AutoComputer {
            return core.computer
        }

        protected abstract fun evalFun(exp: E): () -> Unit
    }

    class AutoBind0<T> : AutoBind<T, () -> T, AutoBind0<T>> {
        constructor(core: AutoBindData<T>) : super(core)

        @Suppress("UNCHECKED_CAST")
        fun <P> with(param: KProperty0<P>): AutoBind1<T, P> {
            val wrapGetter: AGetter<P> = computer().wrapGetter(param) as AGetter<P>
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P> with(target: Any, property: String): AutoBind1<T, P> {
            val wrapGetter: AGetter<P> = computer().wrapBeanProperty<P>(target, property) as AGetter<P>
            return withGetter(wrapGetter)
        }

        fun <P : Any> with(target: Any, property: String, type: KClass<P>): AutoBind1<T, P> {
            return with(target, property)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JTextComponent): AutoBind1<T, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapJTextComponent(param)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JLabel): AutoBind1<T, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapJLabel(param)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun withIndex(param: JList<*>): AutoBind1<T, Int?> {
            val wrapGetter: AGetter<Int?> = computer().wrapJListIndexComponent(param)
            return withGetter(wrapGetter)
        }

        fun withEnable(component: JComponent): AutoBind1<T, Boolean?> {
            val wrapGetter: AGetter<Boolean?> = computer().wrapComponentEnable(component)
            return withGetter(wrapGetter)
        }

        fun withVisible(component: JComponent): AutoBind1<T, Boolean?> {
            val wrapGetter: AGetter<Boolean?> = computer().wrapComponentVisible(component)
            return withGetter(wrapGetter)
        }

        fun withName(component: JComponent): AutoBind1<T, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapComponentName(component)
            return withGetter(wrapGetter)
        }

        fun with(component: JList<*>): AutoBind1<T, List<*>?> {
            val wrapGetter: AGetter<List<*>?> = computer().wrapJListComponent(component)
            return withGetter(wrapGetter)
        }

        fun withIndex(component: JComboBox<*>): AutoBind1<T, Int?> {
            val wrapGetter: AGetter<Int?> = computer().wrapJComboBoxIndexComponent(component)
            return withGetter(wrapGetter)
        }

        fun <P> with(component: JComboBox<P>): AutoBind1<T, P?> {
            val wrapGetter: AGetter<P?> = computer().wrapJComboBoxComponent(component)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P> withGetter(getter: AGetter<P>): AutoBind1<T, P> {
            this.core.params.add(getter as AGetter<Any?>)
            return AutoBind1(core)
        }

        internal fun peakCore(): AutoBindData<T> {
            return core
        }

        override fun evalFun(exp: () -> T): () -> Unit {
            return { computer().value(core.property, exp()) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class AutoBind1<T, P1> : AutoBind<T, (P1) -> T, AutoBind1<T, P1>> {
        constructor(core: AutoBindData<T>) : super(core)

        @Suppress("UNCHECKED_CAST")
        fun <P2> with(param: KProperty0<P2>): AutoBind2<T, P1, P2> {
            val wrapGetter: AGetter<P2> = computer().wrapGetter(param) as AGetter<P2>
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P2> with(target: Any, property: String): AutoBind2<T, P1, P2> {
            val wrapGetter: AGetter<P2> = computer().wrapBeanProperty<P2>(target, property) as AGetter<P2>
            return withGetter(wrapGetter)
        }

        fun <P2 : Any> with(target: Any, property: String, type: KClass<P2>): AutoBind2<T, P1, P2> {
            return with(target, property)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JTextComponent): AutoBind2<T, P1, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapJTextComponent(param)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JLabel): AutoBind2<T, P1, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapJLabel(param)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun withIndex(param: JList<*>): AutoBind2<T, P1, Int?> {
            val wrapGetter: AGetter<Int?> = computer().wrapJListIndexComponent(param)
            return withGetter(wrapGetter)
        }

        fun withEnable(component: JComponent): AutoBind2<T, P1, Boolean?> {
            val wrapGetter: AGetter<Boolean?> = computer().wrapComponentEnable(component)
            return withGetter(wrapGetter)
        }

        fun withVisible(component: JComponent): AutoBind2<T, P1, Boolean?> {
            val wrapGetter: AGetter<Boolean?> = computer().wrapComponentVisible(component)
            return withGetter(wrapGetter)
        }

        fun withName(component: JComponent): AutoBind2<T, P1, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapComponentName(component)
            return withGetter(wrapGetter)
        }

        fun with(component: JList<*>): AutoBind2<T, P1, List<*>?> {
            val wrapGetter: AGetter<List<*>?> = computer().wrapJListComponent(component)
            return withGetter(wrapGetter)
        }

        fun withIndex(component: JComboBox<*>): AutoBind2<T, P1, Int?> {
            val wrapGetter: AGetter<Int?> = computer().wrapJComboBoxIndexComponent(component)
            return withGetter(wrapGetter)
        }

        fun <P2> with(component: JComboBox<P2>): AutoBind2<T, P1, P2?> {
            val wrapGetter: AGetter<P2?> = computer().wrapJComboBoxComponent(component)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P2> withGetter(getter: AGetter<P2>): AutoBind2<T, P1, P2> {
            this.core.params.add(getter as AGetter<Any?>)
            return AutoBind2(core)
        }

        override fun evalFun(exp: (P1) -> T): () -> Unit {
            return {
                computer().value(core.property, exp(core.params[0].get() as P1))
            }
        }
    }

    class AutoBind2<T, P1, P2> : AutoBind<T, (P1?, P2?) -> T?, AutoBind2<T, P1, P2>> {

        constructor(core: AutoBindData<T>) : super(core)

        @Suppress("UNCHECKED_CAST")
        fun <P> with(param: KProperty0<P>): AutoBind3<T, P1, P2, P> {
            val wrapGetter: AGetter<Any?> = computer().wrapGetter(param) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind3<T, P1, P2, P>(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun <P> with(target: Any, property: String): AutoBind3<T, P1, P2, P> {
            val wrapGetter: AGetter<Any?> = computer().wrapBeanProperty<P>(target, property) as AGetter<Any?>
            this.core.params.add(wrapGetter)
            return AutoBind3(core)
        }

        fun <P : Any> with(target: Any, property: String, type: KClass<P>): AutoBind3<T, P1, P2, P> {
            return with(target, property)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JTextComponent): AutoBind3<T, P1, P2, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapJTextComponent(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind3(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun with(param: JLabel): AutoBind3<T, P1, P2, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapJLabel(param)
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind3(core)
        }

        @Suppress("UNCHECKED_CAST")
        fun withIndex(param: JList<Any?>): AutoBind3<T, P1, P2, Int?> {
            val wrapGetter: AGetter<Int?> = computer().wrapJListIndexComponent(param)
            return withGetter(wrapGetter)
        }

        fun withEnable(component: JComponent): AutoBind3<T, P1, P2, Boolean?> {
            val wrapGetter: AGetter<Boolean?> = computer().wrapComponentEnable(component)
            return withGetter(wrapGetter)
        }

        fun withVisible(component: JComponent): AutoBind3<T, P1, P2, Boolean?> {
            val wrapGetter: AGetter<Boolean?> = computer().wrapComponentVisible(component)
            return withGetter(wrapGetter)
        }

        fun withName(component: JComponent): AutoBind3<T, P1, P2, String?> {
            val wrapGetter: AGetter<String?> = computer().wrapComponentName(component)
            return withGetter(wrapGetter)
        }

        fun with(component: JList<*>): AutoBind3<T, P1, P2, List<*>?> {
            val wrapGetter: AGetter<List<*>?> = computer().wrapJListComponent(component)
            return withGetter(wrapGetter)
        }

        fun withIndex(component: JComboBox<*>): AutoBind3<T, P1, P2, Int?> {
            val wrapGetter: AGetter<Int?> = computer().wrapJComboBoxIndexComponent(component)
            return withGetter(wrapGetter)
        }

        fun <P3> with(component: JComboBox<P3>): AutoBind3<T, P1, P2, P3?> {
            val wrapGetter: AGetter<P3?> = computer().wrapJComboBoxComponent(component)
            return withGetter(wrapGetter)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <P3> withGetter(wrapGetter: AGetter<P3>): AutoBind3<T, P1, P2, P3> {
            this.core.params.add(wrapGetter as AGetter<Any?>)
            return AutoBind3(core)
        }

        @Suppress("UNCHECKED_CAST")
        override fun evalFun(exp: (P1?, P2?) -> T?): () -> Unit {
            return {
                computer().value(
                    core.property,
                    exp(this.core.params[0].get() as P1, this.core.params[1].get() as P2)
                )
            }
        }
    }

    class AutoBind3<T, P1, P2, P3>(core: AutoBindData<T>) :
        AutoBind<T, (P1?, P2?, P3?) -> T?, AutoBind3<T, P1, P2, P3>>(core) {

        @Suppress("UNCHECKED_CAST")
        override fun evalFun(exp: (P1?, P2?, P3?) -> T?): () -> Unit {
            return {
                computer().value(
                    core.property, exp(
                        this.core.params[0].get() as P1,
                        this.core.params[1].get() as P2,
                        this.core.params[2].get() as P3
                    )
                )
            }
        }
    }

    class ListenAble<T> {

        private val computer: AutoComputer

        private val wrapGetter: AGetter<T?>

        private var filter: (Filter)? = null

        constructor(computer: AutoComputer, wrapGetter: AGetter<T?>) {
            this.wrapGetter = wrapGetter
            this.computer = computer
        }

        fun throttle(cd: Long): ListenAble<T> {
            val id = IDUtils.shortUUID()
            val throttleFilter = { computer.throttleHelper.acquire(wrapGetter to id, cd) }
            mergeFilter(this::filter, throttleFilter)
            return this
        }

        fun filter(filter: Filter): ListenAble<T> {
            mergeFilter(this::filter, filter)
            return this
        }

        fun action(action: (T?) -> Unit) {
            var buildAction = action
            if (this.filter != null) {
                val wrapAction = buildAction
                val filter = this.filter!!
                buildAction = {
                    if (filter()) {
                        wrapAction(it)
                    }
                }

            }
            this.computer.singleListen(wrapGetter, buildAction)
        }
    }

    companion object {
        fun <T> buildBind(computer: AutoComputer, property: ASetter<T?>): AutoBind0<T> {
            val data: AutoBindData<T> = AutoBindData(computer, property, ArrayList())
            return AutoBind0(data)
        }
    }
}

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
    fun set(value: T?)
}

interface AGetter<T> {
    fun get(): T?

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

    private var hasListen = false

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

class JButtonTextComponentWrap : ASetter<String?>, AGetter<String?> {
    val component: AbstractButton

    constructor(component: AbstractButton) {
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

        other as JButtonTextComponentWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }

    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val jTextComponentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.addChangeListener(ChangeListener {
                if (manual) {
                    return@ChangeListener
                }
                cache = null
                computer.call(jTextComponentWrap)
            })
            hasListen = true
        }
    }
}

class JCheckBoxComponentWrap : ASetter<Boolean?>, AGetter<Boolean?> {
    val component: JCheckBox

    constructor(component: JCheckBox) {
        this.component = component
    }

    @Volatile
    private var cache: Boolean? = null

    @Volatile
    private var manual: Boolean = false

    override fun set(value: Boolean?) {
        cache = value
        EventQueue.invokeLater {
            manual = true
            this.component.isSelected = value ?: false
            manual = false
        }
    }

    override fun get(): Boolean? {
        return when {
            cache != null -> cache
            else -> this.component.isSelected
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JCheckBoxComponentWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }

    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val jCheckBoxComponentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.addActionListener {
                if (manual) {
                    return@addActionListener
                }
                cache = null
                computer.call(jCheckBoxComponentWrap)
            }
            hasListen = true
        }
    }
}

class JLabelWrap : ASetter<String?>, AGetter<String?> {
    private val component: JLabel

    constructor(component: JLabel) {
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

        other as JLabelWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }
}

abstract class NoListenComponentWrap<T> : ASetter<T?>, AGetter<T?> {
    private val component: Component

    constructor(component: Component) {
        this.component = component
    }

    override fun set(value: T?) {
        if (value != null) {
            if (get() != value) {
                EventQueue.invokeLater { set(this.component, value) }
            }
        }
    }

    abstract fun set(component: Component, value: T?)

    override fun get(): T? {
        return get(this.component)
    }

    abstract fun get(component: Component): T?

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoListenComponentWrap<*>

        if (component != other.component) return false

        return true
    }

    abstract fun magicHashCode(): Int

    override fun hashCode(): Int {
        return magicHashCode() xor component.hashCode()
    }
}

class ComponentEnableWrap(component: Component) : NoListenComponentWrap<Boolean>(component) {
    override fun set(component: Component, value: Boolean?) {
        value?.let { component.isEnabled = it }
    }

    override fun get(component: Component): Boolean? {
        return component.isEnabled
    }

    override fun magicHashCode(): Int {
        return "enable".hashCode()
    }
}

class ComponentVisibleWrap(component: Component) : NoListenComponentWrap<Boolean>(component) {
    override fun set(component: Component, value: Boolean?) {
        value?.let { component.isVisible = it }
    }

    override fun get(component: Component): Boolean? {
        return component.isVisible
    }

    override fun magicHashCode(): Int {
        return "visible".hashCode()
    }
}

class ComponentNameWrap(component: Component) : NoListenComponentWrap<String>(component) {
    override fun set(component: Component, value: String?) {
        value?.let { component.name = it }
    }

    override fun get(component: Component): String? {
        return component.name
    }

    override fun magicHashCode(): Int {
        return "name".hashCode()
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
        return when {
            cache != null -> cache
            else -> this.component.selectedIndex
        }
    }


    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val componentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.addListSelectionListener {
                cache = null
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
        return "index".hashCode() xor component.hashCode()
    }
}

class JListComponentWrap : ASetter<List<*>?>, AGetter<List<*>?> {
    private val component: JList<*>

    constructor(component: JList<*>) {
        this.component = component
    }

    @Volatile
    private var cache: List<*>? = null

    @Volatile
    private var manual: Boolean = false

    override fun set(value: List<*>?) {
        if (value != null) {
            cache = value
            EventQueue.invokeLater {
                manual = true
                this.component.model = DefaultComboBoxModel(value.toTypedArray())
                manual = false
            }
        }
    }

    override fun get(): List<*>? {

        if (cache != null) return cache
        val model = this.component.model
        if (model is List<*>) {
            return model
        }
        val modelElements: ArrayList<Any?> = ArrayList()
        (0..model.size).forEach { modelElements.add(model.getElementAt(it)) }
        cache = modelElements
        return modelElements
    }

    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val componentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.model.addListDataListener(object : ListDataListener {
                override fun contentsChanged(e: ListDataEvent?) {
                    cache = null
                    if (!manual) {
                        computer.call(componentWrap)
                    }
                }

                override fun intervalRemoved(e: ListDataEvent?) {
                    cache = null
                    if (!manual) {
                        computer.call(componentWrap)
                    }
                }

                override fun intervalAdded(e: ListDataEvent?) {
                    cache = null
                    if (!manual) {
                        computer.call(componentWrap)
                    }
                }
            })
            hasListen = true
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JListComponentWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return component.hashCode()
    }
}

class JComboBoxComponentIndexWrap : ASetter<Int?>, AGetter<Int?> {
    private val component: JComboBox<*>

    constructor(component: JComboBox<*>) {
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
        return when {
            cache != null -> cache
            else -> this.component.selectedIndex
        }
    }


    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val componentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.addItemListener {
                cache = null
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

        other as JComboBoxComponentIndexWrap

        if (component != other.component) return false

        return true
    }

    override fun hashCode(): Int {
        return "index".hashCode() xor component.hashCode()
    }
}

class JComboBoxComponentWrap<T> : ASetter<T?>, AGetter<T?> {
    private val component: JComboBox<T>

    constructor(component: JComboBox<T>) {
        this.component = component
    }

    @Volatile
    private var cache: T? = null

    @Volatile
    private var manual: Boolean = false

    override fun set(value: T?) {
        if (value != null) {
            EventQueue.invokeLater {
                manual = true
                this.component.selectedItem = value
                manual = false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(): T? {
        if (cache != null) return cache
        cache = this.component.selectedItem as T?
        return cache
    }

    private var hasListen = false

    override fun onListen(computer: AutoComputer) {
        listenChange(computer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenChange(computer: AutoComputer) {
        if (!hasListen) {
            val componentWrap: AGetter<Any?> = this as AGetter<Any?>
            component.model.addListDataListener(object : ListDataListener {
                override fun contentsChanged(e: ListDataEvent?) {
                    cache = null
                    if (!manual) {
                        computer.call(componentWrap)
                    }
                }

                override fun intervalRemoved(e: ListDataEvent?) {
                    cache = null
                    if (!manual) {
                        computer.call(componentWrap)
                    }
                }

                override fun intervalAdded(e: ListDataEvent?) {
                    cache = null
                    if (!manual) {
                        computer.call(componentWrap)
                    }
                }
            })
            hasListen = true
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JComboBoxComponentWrap<*>

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

//region enhance AutoBind0------------------------------------------------------------
// [consistent]: One-to-one direct binding of two identical type values
// [from]:Take a value from the same type property directly
// [option]: Automatic convert source value to Optional
// [eval]:Simplify operation [eval]

fun <T> AutoComputer.AutoBind0<T>.from(param: KProperty0<T>) {
    this.with(param).eval()
}

fun <T> AutoComputer.AutoBind0<T>.from(target: Any, property: String) {
    this.with<T>(target, property).eval()
}

fun AutoComputer.AutoBind0<String?>.from(param: JTextComponent) {
    this.with(param).eval()
}

fun AutoComputer.AutoBind0<String?>.from(param: JLabel) {
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
fun <T> AutoComputer.AutoBind0<T>.mutual(param: KProperty0<T>) {
    with(param).eval { r -> r }
    val core = peakCore()
    if (param is KMutableProperty0 && core.property is AGetter<*>) {
        core.computer.bind(param).withGetter(core.property as AGetter<T>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> AutoComputer.AutoBind0<T>.mutual(target: Any, property: String) {
    with<T>(target, property).eval { r -> r }
    val core = peakCore()
    if (core.property is AGetter<*>) {
        core.computer.bind<T>(target, property).withGetter(core.property as AGetter<T>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun AutoComputer.AutoBind0<String?>.mutual(param: JTextComponent) {
    val core = peakCore()
    val wrapGetter: AGetter<String?> = core.computer.wrapJTextComponent(param)
    withGetter(wrapGetter).eval { s -> s }
    if (core.property is AGetter<*>) {
        core.computer.bind(param).withGetter(core.property as AGetter<String?>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun AutoComputer.AutoBind0<String?>.mutual(param: JLabel) {
    val core = peakCore()
    val wrapGetter: AGetter<String?> = core.computer.wrapJLabel(param)
    withGetter(wrapGetter).eval { s -> s }
    if (core.property is AGetter<*>) {
        core.computer.bind(param).withGetter(core.property as AGetter<String?>).eval { r -> r }
    }
}

@Suppress("UNCHECKED_CAST")
fun AutoComputer.AutoBind0<Int?>.mutual(param: JList<*>) {
    val core = peakCore()
    val wrapGetter: AGetter<Int?> = core.computer.wrapJListIndexComponent(param)
    withGetter(wrapGetter).eval { index -> index }
    if (core.property is AGetter<*>) {
        core.computer.bindIndex(param).withGetter(core.property as AGetter<Int?>).eval { r -> r }
    }
}
//endregion enhance AutoBind0------------------------------------------------------------

object AutoComputerUtils {

    fun mergeFilter(filterProperty: KMutableProperty0<Filter?>, filter: Filter) {
        val oldFilter = filterProperty.get()
        if (oldFilter == null) {
            filterProperty.set(filter)
        } else {
            filterProperty.set {
                oldFilter() && filter()
            }
        }
    }

    fun <T> from(autoBind: AutoComputer.AutoBind0<T>, param: KProperty0<T>) {
        autoBind.from(param)
    }

    fun <T> from(autoBind: AutoComputer.AutoBind0<T>, target: Any, property: String) {
        autoBind.from(target, property)
    }

    fun from(autoBind: AutoComputer.AutoBind0<String?>, param: JTextComponent) {
        autoBind.from(param)
    }

    fun from(autoBind: AutoComputer.AutoBind0<String?>, param: JLabel) {
        autoBind.from(param)
    }

    fun from(autoBind: AutoComputer.AutoBind0<Int?>, param: JList<*>) {
        autoBind.from(param)
    }

    fun <T, P> option(autoBind: AutoComputer.AutoBind1<T, P?>, exp: (Optional<P>) -> T) {
        autoBind.option(exp)
    }

    fun <T, X : T> eval(autoBind: AutoComputer.AutoBind1<T, X>) {
        autoBind.eval()
    }

    fun <T> mutual(autoBind: AutoComputer.AutoBind0<T>, param: KProperty0<T>) {
        autoBind.mutual(param)
    }

    fun <T> mutual(autoBind: AutoComputer.AutoBind0<T>, target: Any, property: String) {
        autoBind.mutual(target, property)
    }

    fun mutual(autoBind: AutoComputer.AutoBind0<String?>, param: JTextComponent) {
        autoBind.mutual(param)
    }

    fun mutual(autoBind: AutoComputer.AutoBind0<String?>, param: JLabel) {
        autoBind.mutual(param)
    }

    fun mutual(autoBind: AutoComputer.AutoBind0<Int?>, param: JList<*>) {
        autoBind.mutual(param)
    }
}

typealias Filter = () -> Boolean