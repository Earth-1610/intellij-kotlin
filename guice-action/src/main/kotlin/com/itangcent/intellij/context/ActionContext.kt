package com.itangcent.intellij.context

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.binder.AnnotatedConstantBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.matcher.Matcher
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.itangcent.common.concurrent.AQSCountLatch
import com.itangcent.common.concurrent.CountLatch
import com.itangcent.common.concurrent.ValueHolder
import com.itangcent.common.exception.ProcessCanceledException
import com.itangcent.common.utils.IDUtils
import com.itangcent.common.utils.ThreadPoolUtils
import com.itangcent.intellij.constant.EventKey
import com.itangcent.intellij.extend.guice.KotlinModule
import com.itangcent.intellij.extend.guice.instance
import com.itangcent.intellij.extend.guice.singleton
import com.itangcent.intellij.logger.Logger
import com.itangcent.intellij.logger.traceError
import org.aopalliance.intercept.MethodInterceptor
import java.awt.EventQueue
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

/**
 * 1.Use a guice injector to manage all generated instances
 * 2.Use a CountLatch to holds the state of the child processes
 */
class ActionContext {

    private val id = IDUtils.shortUUID()

    private val cache = HashMap<String, Any?>()

    private val lock = ReentrantReadWriteLock()

    @Volatile
    private var stopped = false

    @Volatile
    private var locked = false

    private var countLatch: CountLatch = AQSCountLatch()

    private var executorService: ExecutorService = ThreadPoolUtils.createPool(3, 99, ActionContext::class.java)

    //Use guice to manage the current context instance lifecycle and dependencies
    private var injector: Injector

    private constructor(vararg modules: Module) {
        val appendModules: MutableList<Module> = ArrayList()
        appendModules.addAll(modules)
        appendModules.add(ContextModule(this))
        injector = Guice.createInjector(appendModules)!!
    }

    class ContextModule(private var context: ActionContext) : KotlinModule() {
        override fun configure() {
            bindInstance(context)
        }
    }

    //region cache--------------------------------------------------------------
    fun cache(name: String, bean: Any?) {
        checkStatus()
        lock.writeLock().withLock { cache.put(cachePrefix + name, bean) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCache(name: String): T? {
        checkStatus()
        return lock.readLock().withLock { cache[cachePrefix + name] as T? }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> cacheOrCompute(name: String, beanSupplier: () -> T?): T? {
        checkStatus()
        lock.readLock().withLock {
            if (cache.containsKey(cachePrefix + name)) {
                return cache[cachePrefix + name] as T?
            }
        }
        val bean = beanSupplier()
        lock.writeLock().withLock { cache.put(cachePrefix + name, bean) }
        return bean
    }
    //endregion cache--------------------------------------------------------------

    //region event--------------------------------------------------------------
    @Suppress("UNCHECKED_CAST")
    fun on(name: String, event: () -> Unit) {
        checkStatus()
        lock.writeLock().withLock {
            val key = eventPrefix + name
            val oldEvent: Runnable? = cache[key] as Runnable?
            if (oldEvent == null) {
                cache[key] = Runnable {
                    event()
                }
            } else {
                cache[key] = Runnable {
                    oldEvent.run()
                    event()
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun call(name: String) {
        lock.readLock().withLock {
            val event = cache[eventPrefix + name] as Runnable?
            event?.run()
        }
    }
    //endregion event--------------------------------------------------------------

    //region lock and run----------------------------------------------------------------

    //lock current context
    fun lock(): Boolean = lock.writeLock().withLock {
        return if (locked) {
            false
        } else {
            locked = true
            setContext(this)
            true
        }
    }

    fun hold() {
        checkStatus()
        countLatch.down()
    }

    fun unHold() {
        countLatch.up()
    }

    fun runAsync(runnable: Runnable): Future<*>? {
        checkStatus()
        countLatch.down()
        return executorService.submit {
            try {
                setContext(this, 0)
                runnable.run()
            } catch (e: Exception) {
                this.instance(Logger::class).traceError("error in Async", e)
            } finally {
                releaseContext()
                countLatch.up()
            }
        }
    }

    fun runAsync(runnable: () -> Unit): Future<*>? {
        checkStatus()
        countLatch.down()
        return executorService.submit {
            try {
                setContext(this, 0)
                runnable()
            } catch (e: Exception) {
                this.instance(Logger::class).traceError("error in Async", e)
            } finally {
                releaseContext()
                countLatch.up()
            }
        }
    }

    fun <T> callAsync(callable: () -> T): Future<T>? {
        checkStatus()
        countLatch.down()
        val actionContext = this
        return executorService.submit(Callable<T> {
            try {
                setContext(actionContext, 0)
                return@Callable callable()
            } finally {
                releaseContext()
                countLatch.up()
            }
        })
    }

    fun runInSwingUI(runnable: () -> Unit) {
        checkStatus()
        when {
            getFlag() == swingThreadFlag -> runnable()
            EventQueue.isDispatchThread() -> {
                setContext(
                    this,
                    swingThreadFlag
                )
                try {
                    runnable()
                } finally {
                    releaseContext()
                }
            }
            else -> {
                countLatch.down()
                EventQueue.invokeLater {
                    try {
                        setContext(
                            this,
                            swingThreadFlag
                        )
                        runnable()
                    } catch (e: Exception) {
                        this.instance(Logger::class).traceError("error in SwingUI", e)
                    } finally {
                        releaseContext()
                        countLatch.up()
                    }
                }
            }
        }
    }

    fun <T> callInSwingUI(callable: () -> T?): T? {
        checkStatus()
        when {
            getFlag() == swingThreadFlag -> return callable()
            EventQueue.isDispatchThread() -> {
                setContext(
                    this,
                    swingThreadFlag
                )
                return callable()
            }
            else -> {
                countLatch.down()
                val valueHolder: ValueHolder<T> = ValueHolder()
                EventQueue.invokeLater {
                    try {
                        setContext(
                            this,
                            swingThreadFlag
                        )
                        valueHolder.compute { callable() }
                    } finally {
                        releaseContext()
                        countLatch.up()
                    }
                }
                return valueHolder.getData()
            }
        }
    }

    fun runInWriteUI(runnable: () -> Unit) {
        checkStatus()
        if (getFlag() == writeThreadFlag) {
            runnable()
        } else {
            val project = this.instance(Project::class)
            countLatch.down()
            WriteCommandAction.runWriteCommandAction(project, "callInWriteUI", "easy-api", Runnable {
                try {
                    setContext(
                        this,
                        writeThreadFlag
                    )
                    runnable()
                } catch (e: Exception) {
                    this.instance(Logger::class).traceError("error in WriteUI", e)
                } finally {
                    releaseContext()
                    countLatch.up()
                }
            })
        }
    }

    fun <T> callInWriteUI(callable: () -> T?): T? {
        checkStatus()
        if (getFlag() == writeThreadFlag) {
            return callable()
        } else {
            val project = this.instance(Project::class)
            countLatch.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            WriteCommandAction.runWriteCommandAction(project, "callInWriteUI", "easy-api", Runnable {
                try {
                    setContext(
                        this,
                        writeThreadFlag
                    )
                    valueHolder.compute { callable() }
                } finally {
                    releaseContext()
                    countLatch.up()
                }
            })
            return valueHolder.getData()
        }
    }

    fun runInReadUI(runnable: () -> Unit) {
        checkStatus()
        if (getFlag() == readThreadFlag) {
            runnable()
        } else {
            countLatch.down()
            ReadAction.run<Throwable> {
                try {
                    setContext(
                        this,
                        readThreadFlag
                    )
                    runnable()
                } catch (e: Exception) {
                    this.instance(Logger::class).traceError("error in ReadUI", e)
                } finally {
                    releaseContext()
                    countLatch.up()
                }
            }
        }
    }

    fun <T> callInReadUI(callable: () -> T?): T? {
        checkStatus()
        if (getFlag() == readThreadFlag) {
            return callable()
        } else {
            countLatch.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            ReadAction.run<Throwable> {
                try {
                    setContext(
                        this,
                        readThreadFlag
                    )
                    valueHolder.compute { callable() }
                } finally {
                    releaseContext()
                    countLatch.up()
                }
            }
            return valueHolder.getData()
        }
    }

    /**
     * waits on the sub thread for the complete
     * warning:call method as []waitComplete*] will clear ActionContext which bind on current Thread
     * @see ActionContext.waitCompleteAsync
     */
    fun waitComplete() {
        checkStatus()
        releaseContext()
        this.countLatch.waitFor()
        this.call(EventKey.ONCOMPLETED)
        lock.writeLock().withLock {
            this.cache.clear()
            locked = false
        }
    }

    /**
     * waits on the sub thread for the complete
     * warning:call method as []waitComplete*] will clear ActionContext which bind on current Thread
     * @see ActionContext.waitComplete
     */
    fun waitCompleteAsync() {
        checkStatus()
        releaseContext()
        executorService.submit {
            this.countLatch.waitFor()
            this.call(EventKey.ONCOMPLETED)
            lock.writeLock().withLock {
                this.cache.clear()
                locked = false
            }
        }
    }

    //endregion lock and run----------------------------------------------------------------

    //region content object-----------------------------------------------------
    fun <T : Any> instance(kClass: KClass<T>): T {
        return this.injector.instance(kClass)
    }

    fun <T : Any> instance(init: () -> T): T {
        val obj: T = init()
        this.injector.injectMembers(obj)
        return obj
    }

    fun <T : Any> init(obj: T): T {
        this.injector.injectMembers(obj)
        return obj
    }

    //endregion content object-----------------------------------------------------

    //region equals|hashCode|toString-------------------------------------------
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActionContext

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "ActionContext('$id')"
    }
    //endregion equals|hashCode|toString-------------------------------------------

    /**
     * stop current action
     * force shutdown all thread if param shutdown is true
     * todo:call completed event?
     */
    fun stop(shutdown: Boolean = true) {
        Thread {
            stopped = true
            if (shutdown) {
                executorService.shutdown()
            }
        }.start()
    }

    fun isStopped(): Boolean {
        return stopped
    }

    fun checkStatus() {
        if (stopped) {
            throw ProcessCanceledException("Action was stopped")
        }
    }

    companion object {

        private const val readThreadFlag = 0b0001
        private const val writeThreadFlag = 0b0010
        private const val swingThreadFlag = 0b0100

        private const val cachePrefix = "cache_"
        private const val eventPrefix = "event_"

        private var localContext: ThreadLocal<ThreadLocalContext> = ThreadLocal()

        /**
         * Get actionContext in the current thread
         */
        fun getContext(): ActionContext? {
            return localContext.get()?.actionContext
        }

        fun getFlag(): Int {
            return localContext.get()?.flag ?: 0
        }

        fun builder(): ActionContextBuilder {
            val actionContextBuilder = ActionContextBuilder()
            defaultInjects.forEach { it(actionContextBuilder) }
            return actionContextBuilder
        }

        private var defaultInjects: LinkedList<(ActionContextBuilder) -> Unit> = LinkedList()

        fun addDefaultInject(inject: (ActionContextBuilder) -> Unit) {
            defaultInjects.add(inject)
        }

        private fun setContext(actionContext: ActionContext) {
            setContext(
                actionContext,
                getFlag()
            )
        }

        private fun setContext(actionContext: ActionContext, flag: Int) {
            val existContext = localContext.get()
            if (existContext == null) {
                localContext.set(
                    ThreadLocalContext(
                        actionContext,
                        flag,
                        1
                    )
                )
            } else {
                existContext.addCount()
            }
        }

        private fun releaseContext() {

            val existContext = localContext.get()
            if (existContext != null) {
                if (existContext.releaseCount() == 0) {
                    localContext.remove()
                }
            }
        }

        /**
         * Declares a local proxy object that
         * retrieves the corresponding object of this type in context in the thread
         * when used
         */
        inline fun <reified T : Any> local() =
            ThreadLocalContextBeanProxies.instance(T::class)

        fun <T : Any> instance(clazz: KClass<T>): T {
            return ThreadLocalContextBeanProxies.instance(clazz)
        }

        class ThreadLocalContext(val actionContext: ActionContext, val flag: Int, var count: Int) {
            fun addCount() {
                count++
            }

            fun releaseCount(): Int {
                return --count
            }
        }
    }

    /**
     * Allows overridden existing bindings,instead of throwing exceptions
     */
    class ActionContextBuilder : ModuleActions {
        override fun <T : Any> bind(type: Class<T>, callBack: (LinkedBindingBuilder<T>) -> Unit) {
            moduleActions.removeIf {
                (it.size == 3 && it[0] == BIND_INSTANCE_WITH_CLASS && it[1] == type) ||
                        (it.size == 3 && it[0] == BIND && it[1] == type)
            }
            moduleActions.add(arrayOf(BIND, type, callBack))
        }

        override fun <T : Any> bind(
            type: Class<T>,
            annotationType: Class<out Annotation>,
            callBack: (LinkedBindingBuilder<T>) -> Unit
        ) {
            moduleActions.removeIf {
                it.size == 4 && it[0] == BIND_WITH_ANNOTATION_TYPE && it[1] == type && it[2] == annotationType
            }
            moduleActions.add(arrayOf(BIND_WITH_ANNOTATION_TYPE, type, annotationType, callBack))
        }

        override fun <T : Any> bind(
            type: Class<T>,
            annotation: Annotation,
            callBack: (LinkedBindingBuilder<T>) -> Unit
        ) {
            moduleActions.removeIf {
                it.size == 4 && it[0] == BIND_WITH_ANNOTATION && it[1] == type && it[2] == annotation
            }
            moduleActions.add(arrayOf(BIND_WITH_ANNOTATION, type, annotation, callBack))
        }

        override fun <T : Any> bind(type: Class<T>, namedText: String, callBack: (LinkedBindingBuilder<T>) -> Unit) {
            moduleActions.removeIf {
                it.size == 4 && it[0] == BIND_WITH_NAME && it[1] == type && it[2] == namedText
            }
            moduleActions.add(arrayOf(BIND_WITH_NAME, type, namedText, callBack))
        }

        override fun <T : Any> bindInstance(name: String, instance: T) {
            moduleActions.removeIf {
                it.size == 3 && it[0] == BIND_INSTANCE_WITH_NAME && it[1] == name
            }
            moduleActions.add(arrayOf(BIND_INSTANCE_WITH_NAME, name, instance))
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> bindInstance(instance: T) {
            bindInstance(instance::class as KClass<T>, instance)
        }

        override fun <T : Any> bindInstance(cls: Class<T>, instance: T) {
            moduleActions.removeIf {
                (it.size == 3 && it[0] == BIND_INSTANCE_WITH_CLASS && it[1] == cls) ||
                        (it.size == 3 && it[0] == BIND && it[1] == cls)
            }
            moduleActions.add(arrayOf(BIND_INSTANCE_WITH_CLASS, cls, instance))
        }

        override fun bindInterceptor(
            classMatcher: Matcher<in Class<*>>,
            methodMatcher: Matcher<in Method>,
            vararg interceptors: MethodInterceptor
        ) {
            moduleActions.add(arrayOf(BIND_INTERCEPTOR, classMatcher, methodMatcher, interceptors))
        }

        override fun bindConstant(callBack: (AnnotatedConstantBindingBuilder) -> Unit) {
            moduleActions.add(arrayOf(BIND_CONSTANT, callBack))
        }

        private val appendModules: MutableList<Module> = LinkedList()

        private val moduleActions: MutableList<Array<Any>> = LinkedList()

        private val contextActions: MutableList<(ActionContext) -> Unit> = LinkedList()

        fun addModule(vararg modules: Module) {
            this.appendModules.addAll(modules)
        }

        override fun cache(name: String, bean: Any?) {
            contextActions.add { it.cache(name, bean) }
        }

        fun build(): ActionContext {
            if (moduleActions.isNotEmpty()) {
                appendModules.add(ConfiguredModule(ArrayList(moduleActions)))
                moduleActions.clear()
            }
            val actionContext = ActionContext(*appendModules.toTypedArray())
            contextActions.forEach { it(actionContext) }
            return actionContext
        }

        companion object {
            const val BIND_WITH_ANNOTATION_TYPE = "bindWithAnnotationType"
            const val BIND_WITH_ANNOTATION = "bindWithAnnotation"
            const val BIND = "bind"
            const val BIND_WITH_NAME = "bindWithName"
            @Deprecated(
                message = "instead of bindInstanceWithClass",
                replaceWith = ReplaceWith("BIND_INSTANCE_WITH_CLASS")
            )
            const val BIND_INSTANCE = "bindInstance"
            const val BIND_INSTANCE_WITH_CLASS = "bindInstanceWithClass"
            const val BIND_INSTANCE_WITH_NAME = "bindInstanceWithName"
            const val BIND_INTERCEPTOR = "bindInterceptor"
            const val BIND_CONSTANT = "bindConstant"
        }
    }

    class ConfiguredModule(private val moduleActions: MutableList<Array<Any>> = ArrayList()) : KotlinModule() {

        @Suppress("UNCHECKED_CAST")
        override fun configure() {
            super.configure()
            for (moduleAction in moduleActions) {
                when (moduleAction[0]) {
                    ActionContextBuilder.BIND_WITH_ANNOTATION_TYPE -> {
                        (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit))(
                            bind(moduleAction[1] as Class<*>, moduleAction[2] as Class<Annotation>)
                        )
                    }
                    ActionContextBuilder.BIND_WITH_ANNOTATION -> {
                        (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit))(
                            bind(moduleAction[1] as Class<*>, moduleAction[2] as Annotation)
                        )
                    }
                    ActionContextBuilder.BIND_WITH_NAME -> {
                        (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit))(
                            bind(moduleAction[1] as Class<*>, moduleAction[2] as String)
                        )
                    }
                    ActionContextBuilder.BIND_INSTANCE_WITH_NAME -> {
                        bindInstance(moduleAction[1] as String, moduleAction[2])
                    }
                    ActionContextBuilder.BIND_INSTANCE -> {
                        bindInstance(moduleAction[1])
                    }
                    ActionContextBuilder.BIND_INSTANCE_WITH_CLASS -> {
                        bindInstance(moduleAction[1] as Class<Any>, moduleAction[2])
                    }
                    ActionContextBuilder.BIND -> {
                        (moduleAction[2] as ((LinkedBindingBuilder<*>) -> Unit))(
                            bind(moduleAction[1] as Class<*>)
                        )
                    }
                    ActionContextBuilder.BIND_INTERCEPTOR -> {
                        bindInterceptor(
                            moduleAction[1] as Matcher<in Class<*>>?,
                            moduleAction[2] as Matcher<in Method>?,
                            moduleAction[3] as MethodInterceptor?
                        )
                    }
                    ActionContextBuilder.BIND_CONSTANT -> {
                        (moduleAction[1] as ((AnnotatedConstantBindingBuilder) -> Unit)).invoke(
                            bindConstant()
                        )
                    }
                }
            }
        }
    }

    interface ModuleActions {
        fun <T : Any> bind(type: KClass<T>) {
            bind(type) { it.singleton() }
        }

        fun <T : Any> bind(type: Class<T>) {
            bind(type) { it.singleton() }
        }

        fun <T : Any> bind(type: KClass<T>, callBack: (LinkedBindingBuilder<T>) -> Unit) {
            bind(type.java, callBack)
        }

        fun <T : Any> bind(type: Class<T>, callBack: (LinkedBindingBuilder<T>) -> Unit)

        fun <T : Any> bind(
            type: KClass<T>, annotationType: Class<out Annotation>
        ) {
            bind(type, annotationType) { it.singleton() }
        }

        fun <T : Any> bind(
            type: Class<T>, annotationType: Class<out Annotation>
        ) {
            bind(type, annotationType) { it.singleton() }
        }

        fun <T : Any> bind(
            type: KClass<T>,
            annotationType: Class<out Annotation>,
            callBack: ((LinkedBindingBuilder<T>) -> Unit)
        ) {
            bind(type.java, annotationType, callBack)
        }

        fun <T : Any> bind(
            type: Class<T>,
            annotationType: Class<out Annotation>,
            callBack: ((LinkedBindingBuilder<T>) -> Unit)
        )

        fun <T : Any> bind(type: KClass<T>, annotation: Annotation) {
            bind(type, annotation) { it.singleton() }
        }

        fun <T : Any> bind(type: Class<T>, annotation: Annotation) {
            bind(type, annotation) { it.singleton() }
        }

        fun <T : Any> bind(type: KClass<T>, annotation: Annotation, callBack: ((LinkedBindingBuilder<T>) -> Unit)) {
            bind(type.java, annotation, callBack)
        }

        fun <T : Any> bind(type: Class<T>, annotation: Annotation, callBack: ((LinkedBindingBuilder<T>) -> Unit))

        fun <T : Any> bind(type: KClass<T>, namedText: String) {
            bind(type, namedText) { it.singleton() }
        }

        fun <T : Any> bind(type: Class<T>, namedText: String) {
            bind(type, namedText) { it.singleton() }
        }

        fun <T : Any> bind(type: KClass<T>, namedText: String, callBack: ((LinkedBindingBuilder<T>) -> Unit)) {
            bind(type.java, namedText, callBack)
        }

        fun <T : Any> bind(type: Class<T>, namedText: String, callBack: ((LinkedBindingBuilder<T>) -> Unit))

        fun <T : Any> bindInstance(name: String, instance: T)

        fun <T : Any> bindInstance(instance: T)

        fun <T : Any> bindInstance(cls: KClass<T>, instance: T) {
            bindInstance(cls.java, instance)
        }

        fun <T : Any> bindInstance(cls: Class<T>, instance: T)

        fun cache(name: String, bean: Any?)

        fun bindInterceptor(
            classMatcher: Matcher<in Class<*>>,
            methodMatcher: Matcher<in Method>,
            vararg interceptors: org.aopalliance.intercept.MethodInterceptor
        )

        fun bindConstant(callBack: (AnnotatedConstantBindingBuilder) -> Unit)
    }
}
