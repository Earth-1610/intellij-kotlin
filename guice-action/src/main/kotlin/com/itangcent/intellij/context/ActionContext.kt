package com.itangcent.intellij.context

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.binder.AnnotatedConstantBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.matcher.Matcher
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.itangcent.common.concurrent.AQSCountLatch
import com.itangcent.common.concurrent.CountLatch
import com.itangcent.common.concurrent.ValueHolder
import com.itangcent.common.exception.ProcessCanceledException
import com.itangcent.common.logger.Log
import com.itangcent.common.logger.traceError
import com.itangcent.common.spi.Setup
import com.itangcent.common.spi.SpiUtils
import com.itangcent.common.utils.IDUtils
import com.itangcent.common.utils.ThreadPoolUtils
import com.itangcent.common.utils.safe
import com.itangcent.intellij.CustomInfo
import com.itangcent.intellij.constant.EventKey
import com.itangcent.intellij.context.ActionContext.*
import com.itangcent.intellij.extend.guice.*
import com.itangcent.intellij.logger.Logger
import org.aopalliance.intercept.MethodInterceptor
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import java.awt.EventQueue
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass

/**
 * 1.Use a guice injector to manage all generated instances
 * 2.Use a CountLatch to holds the state of the child processes
 */
class ActionContext {

    private val id = IDUtils.shortUUID()

    private val cache = HashMap<String, Any?>()

    private val lock = ReentrantReadWriteLock()

    private val activeThreadCnt = Array(ThreadFlag.values().size) { AtomicInteger() }

    internal lateinit var mainBoundary: InnerBoundary

    private lateinit var rootContextStatus: ContextStatus

    private var executorService: ExecutorService =
        ThreadPoolUtils.newCachedThreadPool(
            BasicThreadFactory.Builder().daemon(true)
                .namingPattern("ActionContext-%d").build()
        )

    //Use guice to manage the current context instance lifecycle and dependencies
    private var injector: Injector

    internal constructor(vararg modules: Module) {
        val appendModules: MutableList<Module> = ArrayList()
        appendModules.addAll(modules)
        appendModules.add(ContextModule(this))
        initContextStatus()
        injector = Guice.createInjector(appendModules)!!
    }

    private fun initContextStatus() {
        this.rootContextStatus = ContextStatus(this, 0)
        localContextStatus.set(rootContextStatus)
    }

    class ContextModule(private var context: ActionContext) : KotlinModule() {
        override fun configure() {
            bindInstance(context)
        }
    }

    //region cache--------------------------------------------------------------

    fun cache(name: String, bean: Any?) {
        LOG.info("cache [$name]")
        checkStatus()
        lock.write {
            cache[cachePrefix + name] = bean
            LOG.info("cache [$name] success")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCache(name: String): T? {
        checkStatus()
        return lock.read { cache[cachePrefix + name] as T? }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> deleteCache(name: String): T? {
        checkStatus()
        lock.write {
            return cache.remove(cachePrefix + name) as T?
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> cacheOrCompute(name: String, beanSupplier: () -> T?): T? {
        LOG.info("compute cache [$name]")
        checkStatus()
        lock.read {
            cache[cachePrefix + name] as? T
        }?.let { return it }
        val bean = beanSupplier()
        lock.write {
            if (cache.containsKey(cachePrefix + name)) {
                return cache[cachePrefix + name] as T?
            }
            cache.put(cachePrefix + name, bean)
        }
        return bean
    }

    //endregion cache--------------------------------------------------------------

    //region event--------------------------------------------------------------
    @Suppress("UNCHECKED_CAST")
    fun on(name: String, event: ActionContextEvent) {
        LOG.info("register event [$name]")
        checkStatus()
        lock.write {
            val key = eventPrefix + name
            val oldEvent: ActionContextEvent? = cache[key] as ActionContextEvent?
            if (oldEvent == null) {
                cache[key] = event
            } else {
                cache[key] = oldEvent.and(event)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun call(name: String) {
        LOG.info("call event [$name]")
        val event = lock.read {
            cache[eventPrefix + name] as? ActionContextEvent
        }
        event?.invoke(this)
    }

    //endregion event--------------------------------------------------------------

    //region lock and run----------------------------------------------------------------

    fun hold() {
        checkStatus()
        this.mainBoundary.down()
    }

    fun unHold() {
        this.mainBoundary.up()
    }

    fun runAsync(runnable: Runnable): Future<*>? {
        checkStatus()
        val contextStatus = getContextStatus()
        val boundaries = contextStatus.boundaries()
        boundaries?.down()
        activeThreadCnt[ThreadFlag.ASYNC.ordinal].getAndIncrement()
        return executorService.submit {
            try {
                checkStatus()
                setContext(contextStatus, ThreadFlag.ASYNC)
                runnable.run()
            } catch (_: ProcessCanceledException) {
            } catch (e: Exception) {
                this.instance(Logger::class).traceError("error in Async", e)
            } finally {
                boundaries?.up()
                releaseContext()
                activeThreadCnt[ThreadFlag.ASYNC.ordinal].getAndDecrement()
            }
        }
    }

    fun runAsync(runnable: () -> Unit): Future<*>? {
        checkStatus()
        val contextStatus = getContextStatus()
        val boundaries = contextStatus.boundaries()
        boundaries?.down()
        activeThreadCnt[ThreadFlag.ASYNC.ordinal].getAndIncrement()
        return executorService.submit {
            try {
                checkStatus()
                setContext(contextStatus, ThreadFlag.ASYNC)
                runnable()
            } catch (_: ProcessCanceledException) {
            } catch (e: Exception) {
                this.instance(Logger::class).traceError("error in Async", e)
            } finally {
                boundaries?.up()
                releaseContext()
                activeThreadCnt[ThreadFlag.ASYNC.ordinal].getAndDecrement()
            }
        }
    }

    fun <T> callAsync(callable: () -> T): Future<T>? {
        checkStatus()
        val contextStatus = getContextStatus()
        val boundaries = contextStatus.boundaries()
        boundaries?.down()
        activeThreadCnt[ThreadFlag.ASYNC.ordinal].getAndIncrement()
        return executorService.submit(Callable {
            try {
                checkStatus()
                setContext(contextStatus, ThreadFlag.ASYNC)
                return@Callable callable()
            } catch (e: ProcessCanceledException) {
                return@Callable null
            } finally {
                boundaries?.up()
                releaseContext()
                activeThreadCnt[ThreadFlag.ASYNC.ordinal].getAndDecrement()
            }
        })
    }

    fun runInSwingUI(runnable: () -> Unit) {
        checkStatus()
        when {
            getFlag() == ThreadFlag.SWING.value -> runnable()
            EventQueue.isDispatchThread() -> {
                val contextStatus = getContextStatus()
                setContext(
                    contextStatus,
                    ThreadFlag.SWING
                )
                activeThreadCnt[ThreadFlag.SWING.ordinal].getAndIncrement()
                try {
                    runnable()
                } finally {
                    releaseContext()
                    activeThreadCnt[ThreadFlag.SWING.ordinal].getAndDecrement()
                }
            }

            else -> {
                val contextStatus = getContextStatus()
                val boundaries = contextStatus.boundaries()
                boundaries?.down()
                EventQueue.invokeLater {
                    try {
                        activeThreadCnt[ThreadFlag.SWING.ordinal].getAndIncrement()
                        checkStatus()
                        setContext(
                            contextStatus,
                            ThreadFlag.SWING
                        )
                        runnable()
                    } catch (_: ProcessCanceledException) {
                    } catch (e: Exception) {
                        this.instance(Logger::class).traceError("error in SwingUI", e)
                    } finally {
                        boundaries?.up()
                        releaseContext()
                        activeThreadCnt[ThreadFlag.SWING.ordinal].getAndDecrement()
                    }
                }
            }
        }
    }

    fun <T> callInSwingUI(callable: () -> T?): T? {
        checkStatus()
        when {
            getFlag() == ThreadFlag.SWING.value -> return callable()
            EventQueue.isDispatchThread() -> {
                val contextStatus = getContextStatus()
                try {
                    activeThreadCnt[ThreadFlag.SWING.ordinal].getAndIncrement()
                    setContext(
                        contextStatus,
                        ThreadFlag.SWING
                    )
                    return callable()
                } finally {
                    releaseContext()
                    activeThreadCnt[ThreadFlag.SWING.ordinal].getAndDecrement()
                }
            }

            else -> {
                val contextStatus = getContextStatus()
                val boundaries = contextStatus.boundaries()
                boundaries?.down()
                val valueHolder: ValueHolder<T> = ValueHolder()
                EventQueue.invokeLater {
                    try {
                        activeThreadCnt[ThreadFlag.SWING.ordinal].getAndIncrement()
                        setContext(
                            contextStatus,
                            ThreadFlag.SWING
                        )
                        valueHolder.compute {
                            checkStatus()
                            callable()
                        }
                    } catch (e: Throwable) {
                        valueHolder.failed(e)
                    } finally {
                        boundaries?.up()
                        releaseContext()
                        activeThreadCnt[ThreadFlag.SWING.ordinal].getAndDecrement()
                    }
                }
                return valueHolder.value()
            }
        }
    }

    private val pluginName: String by lazy {
        SpiUtils.loadService(CustomInfo::class)?.pluginName() ?: "intellij-plugin"
    }

    fun runInWriteUI(runnable: () -> Unit) {
        checkStatus()
        if (getFlag() == ThreadFlag.WRITE.value) {
            runnable()
        } else {
            val project = this.instance(Project::class)
            val contextStatus = getContextStatus()
            val boundaries = contextStatus.boundaries()
            boundaries?.down()
            try {
                WriteCommandAction.runWriteCommandAction(
                    project, "CallInWriteUI", pluginName, {
                        try {
                            activeThreadCnt[ThreadFlag.WRITE.ordinal].getAndIncrement()
                            checkStatus()
                            setContext(
                                contextStatus,
                                ThreadFlag.WRITE
                            )
                            runnable()
                        } catch (_: ProcessCanceledException) {
                        } catch (e: Exception) {
                            this.instance(Logger::class).traceError("error in WriteUI", e)
                        } finally {
                            boundaries?.up()
                            releaseContext()
                            activeThreadCnt[ThreadFlag.WRITE.ordinal].getAndDecrement()
                        }
                    })
            } catch (e: Throwable) {
                releaseContext()
            }
        }
    }

    fun <T> callInWriteUI(callable: () -> T?): T? {
        checkStatus()
        if (getFlag() == ThreadFlag.WRITE.value) {
            return callable()
        } else {
            val project = this.instance(Project::class)
            val contextStatus = getContextStatus()
            val boundaries = contextStatus.boundaries()
            boundaries?.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            WriteCommandAction.runWriteCommandAction(
                project, "CallInWriteUI",
                pluginName,
                {
                    try {
                        activeThreadCnt[ThreadFlag.WRITE.ordinal].getAndIncrement()
                        setContext(
                            contextStatus,
                            ThreadFlag.WRITE
                        )
                        valueHolder.compute {
                            checkStatus()
                            callable()
                        }
                    } catch (e: Throwable) {
                        valueHolder.failed(e)
                    } finally {
                        boundaries?.up()
                        releaseContext()
                        activeThreadCnt[ThreadFlag.WRITE.ordinal].getAndDecrement()
                    }
                })
            return valueHolder.value()
        }
    }

    fun runInReadUI(runnable: () -> Unit) {
        checkStatus()
        if (getFlag() == ThreadFlag.READ.value) {
            runnable()
        } else {
            val contextStatus = getContextStatus()
            val boundaries = contextStatus.boundaries()
            boundaries?.down()
            ReadAction.run<Throwable> {
                try {
                    activeThreadCnt[ThreadFlag.READ.ordinal].getAndIncrement()
                    setContext(
                        contextStatus,
                        ThreadFlag.READ
                    )
                    checkStatus()
                    runnable()
                } catch (_: ProcessCanceledException) {
                } catch (e: Exception) {
                    this.instance(Logger::class).traceError("error in ReadUI", e)
                } finally {
                    boundaries?.up()
                    releaseContext()
                    activeThreadCnt[ThreadFlag.READ.ordinal].getAndDecrement()
                }
            }
        }
    }

    fun <T> callInReadUI(callable: () -> T?): T? {
        checkStatus()
        if (getFlag() == ThreadFlag.READ.value) {
            return callable()
        } else {
            val contextStatus = getContextStatus()
            val boundaries = contextStatus.boundaries()
            boundaries?.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            ReadAction.run<Throwable> {
                try {
                    activeThreadCnt[ThreadFlag.READ.ordinal].getAndIncrement()
                    setContext(
                        contextStatus,
                        ThreadFlag.READ
                    )
                    valueHolder.compute {
                        checkStatus()
                        callable()
                    }
                } catch (e: Throwable) {
                    valueHolder.failed(e)
                } finally {
                    boundaries?.up()
                    releaseContext()
                    activeThreadCnt[ThreadFlag.READ.ordinal].getAndDecrement()
                }
            }
            return valueHolder.value()
        }
    }

    fun runInAWT(runnable: () -> Unit) {
        checkStatus()
        if (getFlag() == ThreadFlag.AWT.value) {
            runnable()
        } else {
            val contextStatus = getContextStatus()
            val boundaries = contextStatus.boundaries()
            boundaries?.down()
            ApplicationManager.getApplication().invokeLater {
                try {
                    activeThreadCnt[ThreadFlag.AWT.ordinal].getAndIncrement()
                    setContext(
                        contextStatus,
                        ThreadFlag.AWT
                    )
                    checkStatus()
                    runnable()
                } catch (e: ProcessCanceledException) {
                } catch (e: Exception) {
                    this.instance(Logger::class).traceError("error in AWT UI", e)
                } finally {
                    boundaries?.up()
                    releaseContext()
                    activeThreadCnt[ThreadFlag.AWT.ordinal].getAndDecrement()
                }
            }
        }
    }

    fun <T> callInAWT(callable: () -> T?): T? {
        checkStatus()
        if (getFlag() == ThreadFlag.AWT.value) {
            return callable()
        } else {
            val contextStatus = getContextStatus()
            val boundaries = contextStatus.boundaries()
            boundaries?.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            ApplicationManager.getApplication().invokeLater {
                try {
                    activeThreadCnt[ThreadFlag.AWT.ordinal].getAndIncrement()
                    setContext(
                        contextStatus,
                        ThreadFlag.AWT
                    )
                    valueHolder.compute {
                        checkStatus()
                        callable()
                    }
                } catch (e: Throwable) {
                    valueHolder.failed(e)
                } finally {
                    boundaries?.up()
                    releaseContext()
                    activeThreadCnt[ThreadFlag.AWT.ordinal].getAndDecrement()
                }
            }
            return valueHolder.value()
        }
    }

    /**
     * Blocks until all sub thread have completed terminated.
     * warning:call method as [waitComplete*] will clear ActionContext which bind on current Thread
     * @see ActionContext.waitCompleteAsync
     */
    fun waitComplete() {
        try {
            if (this.isStopped()) {
                return
            }
            checkStatus()
            releaseContext()
            this.mainBoundary.waitComplete()
        } finally {
            stop()
        }
    }

    /**
     * waits on the sub thread for the complete
     * warning:call method as [waitComplete*] will clear ActionContext which bind on current Thread
     * @see ActionContext.waitComplete
     */
    fun waitCompleteAsync() {
        checkStatus()
        releaseContext()
        executorService.submit {
            try {
                this.mainBoundary.waitComplete()
            } finally {
                stop()
            }
        }
    }

    fun createBoundary(): Boundary {
        val contextStatus = getContextStatus()
        val boundary = BoundaryImpl(contextStatus)
        contextStatus.addBoundary(boundary)
        return boundary
    }

    //endregion lock and run----------------------------------------------------------------

    //region content object-----------------------------------------------------
    fun <T : Any> instance(kClass: KClass<T>): T {
        return this.injector.instance(kClass)
    }

    /**
     * Tries to get an instance of the specified type, returning null if not available
     * instead of throwing an exception.
     */
    fun <T : Any> tryInstance(kClass: KClass<T>): T? {
        return safe {
            this.injector.instance(kClass)
        }
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
    fun stop() {
        Thread {
            try {
                safe { this.call(EventKey.ON_COMPLETED) }
                safe {
                    lock.write {
                        this.cache.clear()
                    }
                }
                safe { this.mainBoundary.close() }
                this.rootContextStatus.flag = ThreadFlag.INVALID.value
            } finally {
                executorService.shutdown()
            }
        }.start()
    }

    fun isStopped(): Boolean {
        return this.mainBoundary.isClosed()
    }

    fun checkStatus() {
        if (isStopped() || (localContextStatus.get()?.boundaries()?.isClosed() == true)) {
            throw ProcessCanceledException("ActionContext was stopped")
        }
    }

    fun activeThreads(): Int {
        return activeThreadCnt.sumOf { it.get() }
    }

    fun activeThreads(threadFlag: ThreadFlag): Int {
        return activeThreadCnt[threadFlag.ordinal].get()
    }

    internal fun onStart() {
        this.call(EventKey.ON_START)
    }

    private var parentActionContext: ActionContext? = null

    internal fun setParentContext(actionContext: ActionContext) {
        parentActionContext = actionContext
    }

    fun parentActionContext(): ActionContext? {
        return this.parentActionContext
    }

    private fun isReady(): Boolean {
        return this.injector != null && !isStopped()
    }

    companion object {

        private const val cachePrefix = "cache_"
        private const val eventPrefix = "event_"

        private var localContextStatus: ThreadLocal<ContextStatus> = ThreadLocal()

        /**
         * Get actionContext in the current thread
         */
        fun getContext(): ActionContext? {
            return getLocalContextStatus()?.actionContext?.takeIf { it.isReady() }
        }

        fun getFlag(): Int {
            return getLocalContextStatus()?.flag ?: 0
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

        fun removeDefaultInject(inject: (ActionContextBuilder) -> Unit) {
            defaultInjects.remove(inject)
        }

        private fun setContext(contextStatus: ContextStatus, flag: ThreadFlag) {
            localContextStatus.set(createContextStatus(contextStatus, flag.value))
        }

        private fun createContextStatus(
            contextStatus: ContextStatus,
            flag: Int
        ): ContextStatus {
            val existContext = getLocalContextStatus()
            val subContextStatus: ContextStatus = if (existContext == contextStatus) {
                //in one thread
                ContextStatus(contextStatus.actionContext, flag, existContext)
            } else {
                ContextStatus(contextStatus.actionContext, flag)
            }
            contextStatus.boundaries()?.let {
                subContextStatus.addBoundary(it)
            }
            return subContextStatus
        }

        private fun releaseContext() {
            getLocalContextStatus()?.release()
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

        class ContextStatus(
            val actionContext: ActionContext,
            var flag: Int,
            var parent: ContextStatus? = null
        ) {
            private var boundaries: LinkedList<InnerBoundary>? = null
            private var unionBoundary: InnerBoundary? = null

            fun release() {
                if (parent == null) {
                    localContextStatus.remove()
                } else {
                    localContextStatus.set(parent)
                }
            }

            fun boundaries(): InnerBoundary? {
                var boundary = unionBoundary
                if (boundary == null) {
                    boundary = buildUnionBoundary()
                    this.unionBoundary = boundary
                }
                return boundary!!
            }

            private fun buildUnionBoundary(): InnerBoundary? {
                this.boundaries?.removeIf { it.removed() }
                val bs = boundaries
                return when {
                    bs.isNullOrEmpty() -> {
                        null
                    }

                    bs.size == 1 -> {
                        bs.first
                    }

                    else -> InnerBoundaries(LinkedList(bs))
                }
            }

            fun addBoundary(boundary: InnerBoundary) {
                if (this.boundaries == null) {
                    this.boundaries = LinkedList()
                }
                this.boundaries!!.add(boundary)
                this.unionBoundary = null
            }

            fun removeBoundary(boundary: InnerBoundary) {
                if (getLocalContextStatus() == this) {
                    this.boundaries!!.remove(boundary)
                }
                this.unionBoundary = null
            }
        }

        private fun getLocalContextStatus(): ContextStatus? {
            val contextStatus = localContextStatus.get()
            return when {
                contextStatus == null -> {
                    null
                }

                contextStatus.flag == ThreadFlag.INVALID.value -> {
                    localContextStatus.remove()
                    null
                }

                else -> contextStatus
            }
        }
    }

    private fun getContextStatus(): ContextStatus {
        return getLocalContextStatus() ?: rootContextStatus
    }

    enum class BindAction(val key: String) {
        BIND_WITH_ANNOTATION_TYPE("bindWithAnnotationType"),
        BIND_WITH_ANNOTATION("bindWithAnnotation"),
        BIND("bind"),
        BIND_WITH_NAME("bindWithName"),
        BIND_INSTANCE("bindInstance"),
        BIND_INSTANCE_WITH_CLASS("bindInstanceWithClass"),
        BIND_INSTANCE_WITH_GENERATOR("bindInstanceWithGenerator"),
        BIND_INSTANCE_WITH_NAME("bindInstanceWithName"),
        BIND_INTERCEPTOR("bindInterceptor"),
        BIND_CONSTANT("bindConstant"),
        BIND_METHOD_HANDLER("bindMethodHandler"),
        BIND_FIELD_HANDLER("bindFieldHandler")
    }

    class ConfiguredModule(
        private val actionContextBuilder: ActionContextBuilder,
        private val moduleActions: MutableList<Array<Any>> = ArrayList()
    ) : KotlinModule() {

        @Suppress("UNCHECKED_CAST")
        override fun configure() {
            super.configure()
            moduleActions.asSequence()
                .sortedBy { (it[0] as BindAction).ordinal }
                .forEach { moduleAction ->
                    when (moduleAction[0]) {
                        BindAction.BIND_WITH_ANNOTATION_TYPE -> {
                            (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit))(
                                bind(moduleAction[1] as Class<*>, moduleAction[2] as Class<Annotation>)
                            )
                        }

                        BindAction.BIND_WITH_ANNOTATION -> {
                            (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit))(
                                bind(moduleAction[1] as Class<*>, moduleAction[2] as Annotation)
                            )
                        }

                        BindAction.BIND_WITH_NAME -> {
                            (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit))(
                                bind(moduleAction[1] as Class<*>, moduleAction[2] as String)
                            )
                        }

                        BindAction.BIND_INSTANCE_WITH_NAME -> {
                            bindInstance(moduleAction[1] as String, moduleAction[2])
                        }

                        BindAction.BIND_INSTANCE -> {
                            bindInstance(moduleAction[1])
                        }

                        BindAction.BIND_INSTANCE_WITH_CLASS -> {
                            val injectClass = moduleAction[1] as Class<Any>
                            val instance = moduleAction[2]
                            bindInstance(injectClass, processBean(injectClass, instance))
                        }

                        BindAction.BIND_INSTANCE_WITH_GENERATOR -> {
                            val injectClass = moduleAction[1] as Class<Any>
                            val instance = (moduleAction[2] as () -> Any).invoke()
                            bindInstance(injectClass, processBean(injectClass, instance))
                        }

                        BindAction.BIND -> {
                            (moduleAction[2] as ((LinkedBindingBuilder<*>) -> Unit))(
                                try {
                                    bind(moduleAction[1] as Class<*>)
                                } catch (e: Exception) {
                                    TODO("Not yet implemented")
                                }
                            )
                        }

                        BindAction.BIND_INTERCEPTOR -> {
                            bindInterceptor(
                                moduleAction[1] as Matcher<in Class<*>>?,
                                moduleAction[2] as Matcher<in Method>?,
                                *(moduleAction[3] as Array<MethodInterceptor>)
                            )
                        }

                        BindAction.BIND_CONSTANT -> {
                            (moduleAction[1] as ((AnnotatedConstantBindingBuilder) -> Unit)).invoke(
                                bindConstant()
                            )
                        }

                        BindAction.BIND_METHOD_HANDLER -> {
                            bindMethodHandler(
                                moduleAction[1] as Class<Annotation>,
                                moduleAction[2] as MethodHandler<Any?, Annotation>
                            )
                        }

                        BindAction.BIND_FIELD_HANDLER -> {
                            bindFieldHandler(
                                moduleAction[1] as Class<Annotation>,
                                moduleAction[2] as FieldHandler<Any?, Annotation>
                            )
                        }
                    }
                }
        }

        private fun processBean(injectClass: Class<Any>, bean: Any): Any {
            if (Proxy.isProxyClass(bean.javaClass)) {
                return bean
            }
            val interceptors = this.actionContextBuilder.getInterceptorFor(injectClass)
            if (interceptors.isEmpty()) {
                return bean
            }
            return Proxy.newProxyInstance(
                injectClass.classLoader,
                arrayOf(injectClass),
                EnhancedInvocationHandler(DelegateInvocationHandler(bean), interceptors)
            )
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

        fun <T : Any> bindInstanceWith(cls: KClass<T>, instance: () -> T) {
            bindInstanceWith(cls.java, instance)
        }

        fun <T : Any> bindInstance(cls: Class<T>, instance: T)

        fun <T : Any> bindInstanceWith(cls: Class<T>, instance: () -> T)

        fun cache(name: String, bean: Any?)

        fun addAction(action: (ActionContext) -> Unit)

        fun bindInterceptor(
            classMatcher: Matcher<in Class<*>>,
            methodMatcher: Matcher<in Method>,
            vararg interceptors: org.aopalliance.intercept.MethodInterceptor
        )

        fun bindConstant(callBack: (AnnotatedConstantBindingBuilder) -> Unit)

        fun <A : Annotation> bindMethodHandler(
            annotationType: Class<A>,
            methodHandler: MethodHandler<Any?, Annotation>
        )

        fun <A : Annotation> bindFieldHandler(
            annotationType: Class<A>,
            fieldHandler: FieldHandler<Any?, Annotation>
        )
    }
}

typealias ActionContextEvent = (ActionContext) -> Unit

class CombinedActionContextEvent(
    internal val events: List<ActionContextEvent>
) : ActionContextEvent {
    override fun invoke(actionContext: ActionContext) {
        events.forEach { it(actionContext) }
    }
}

fun ActionContextEvent.and(event: ActionContextEvent): ActionContextEvent {
    val events = if (this is CombinedActionContextEvent) {
        this.events + event
    } else {
        listOf(this, event)
    }
    return CombinedActionContextEvent(events)
}

/**
 * Allows overridden existing bindings,instead of throwing exceptions
 */
class ActionContextBuilder : ModuleActions {

    override fun <T : Any> bind(type: Class<T>, callBack: (LinkedBindingBuilder<T>) -> Unit) {
        moduleActions.removeIf {
            (it.size == 3 && it[0] == BindAction.BIND_INSTANCE_WITH_CLASS && it[1] == type) ||
                    (it.size == 3 && it[0] == BindAction.BIND && it[1] == type)
        }
        moduleActions.add(arrayOf(BindAction.BIND, type, callBack))
    }

    override fun <T : Any> bind(
        type: Class<T>,
        annotationType: Class<out Annotation>,
        callBack: (LinkedBindingBuilder<T>) -> Unit
    ) {
        moduleActions.removeIf {
            it.size == 4 && it[0] == BindAction.BIND_WITH_ANNOTATION_TYPE && it[1] == type && it[2] == annotationType
        }
        moduleActions.add(arrayOf(BindAction.BIND_WITH_ANNOTATION_TYPE, type, annotationType, callBack))
    }

    override fun <T : Any> bind(
        type: Class<T>,
        annotation: Annotation,
        callBack: (LinkedBindingBuilder<T>) -> Unit
    ) {
        moduleActions.removeIf {
            it.size == 4 && it[0] == BindAction.BIND_WITH_ANNOTATION && it[1] == type && it[2] == annotation
        }
        moduleActions.add(arrayOf(BindAction.BIND_WITH_ANNOTATION, type, annotation, callBack))
    }

    override fun <T : Any> bind(type: Class<T>, namedText: String, callBack: (LinkedBindingBuilder<T>) -> Unit) {
        moduleActions.removeIf {
            it.size == 4 && it[0] == BindAction.BIND_WITH_NAME && it[1] == type && it[2] == namedText
        }
        moduleActions.add(arrayOf(BindAction.BIND_WITH_NAME, type, namedText, callBack))
    }

    override fun <T : Any> bindInstance(name: String, instance: T) {
        moduleActions.removeIf {
            it.size == 3 && it[0] == BindAction.BIND_INSTANCE_WITH_NAME && it[1] == name
        }
        moduleActions.add(arrayOf(BindAction.BIND_INSTANCE_WITH_NAME, name, instance))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> bindInstance(instance: T) {
        bindInstance(instance::class as KClass<T>, instance)
    }

    override fun <T : Any> bindInstance(cls: Class<T>, instance: T) {
        moduleActions.removeIf {
            (it.size == 3 && it[0] == BindAction.BIND_INSTANCE_WITH_CLASS && it[1] == cls) ||
                    (it.size == 3 && it[0] == BindAction.BIND && it[1] == cls)
        }
        moduleActions.add(arrayOf(BindAction.BIND_INSTANCE_WITH_CLASS, cls, instance))
    }

    override fun <T : Any> bindInstanceWith(cls: Class<T>, instance: () -> T) {
        moduleActions.add(arrayOf(BindAction.BIND_INSTANCE_WITH_GENERATOR, cls, instance))
    }

    override fun bindInterceptor(
        classMatcher: Matcher<in Class<*>>,
        methodMatcher: Matcher<in Method>,
        vararg interceptors: MethodInterceptor
    ) {
        moduleActions.add(arrayOf(BindAction.BIND_INTERCEPTOR, classMatcher, methodMatcher, interceptors))
    }

    override fun bindConstant(callBack: (AnnotatedConstantBindingBuilder) -> Unit) {
        moduleActions.add(arrayOf(BindAction.BIND_CONSTANT, callBack))
    }

    override fun <A : Annotation> bindMethodHandler(
        annotationType: Class<A>,
        methodHandler: MethodHandler<Any?, Annotation>
    ) {
        moduleActions.removeIf {
            it.size == 3 && it[0] == BindAction.BIND_METHOD_HANDLER && it[1] == annotationType
        }
        moduleActions.add(arrayOf(BindAction.BIND_METHOD_HANDLER, annotationType, methodHandler))
    }

    override fun <A : Annotation> bindFieldHandler(
        annotationType: Class<A>,
        fieldHandler: FieldHandler<Any?, Annotation>
    ) {
        moduleActions.removeIf {
            it.size == 3 && it[0] == BindAction.BIND_FIELD_HANDLER && it[1] == annotationType
        }
        moduleActions.add(arrayOf(BindAction.BIND_FIELD_HANDLER, annotationType, fieldHandler))
    }

    private val appendModules: MutableList<Module> = LinkedList()

    private val moduleActions: MutableList<Array<Any>> = LinkedList()

    private val contextActions: MutableList<(ActionContext) -> Unit> = LinkedList()

    fun setParentContext(actionContext: ActionContext) {
        contextActions.add { it.setParentContext(actionContext) }
    }

    fun <T : Any> inheritFrom(parent: ActionContext, cls: KClass<T>) {
        this.bindInstance(cls, parent.instance(cls))
    }

    fun addModule(vararg modules: Module) {
        this.appendModules.addAll(modules)
    }

    override fun cache(name: String, bean: Any?) {
        contextActions.add { it.cache(name, bean) }
    }

    override fun addAction(action: (ActionContext) -> Unit) {
        contextActions.add(action)
    }

    @Suppress("UNCHECKED_CAST")
    fun getInterceptorFor(injectClass: Class<*>): List<MethodInterceptor> {
        return moduleActions.asSequence()
            .filter { it[0] == BindAction.BIND_INTERCEPTOR }
            .filter { (it[1] as Matcher<in Class<*>>).matches(injectClass) }
            .map { it[3] as Array<MethodInterceptor> }
            .flatMap { it.asSequence() }
            .toList()
    }

    fun build(): ActionContext {
        if (moduleActions.isNotEmpty()) {
            appendModules.add(ConfiguredModule(this, ArrayList(moduleActions)))
        }

        val actionContext = ActionContext(*appendModules.toTypedArray())
        actionContext.mainBoundary = actionContext.createBoundary() as InnerBoundary
        contextActions.forEach { it(actionContext) }
        actionContext.runAsync {
            actionContext.onStart()
        }

        return actionContext
    }

    companion object : Log() {
        init {
            Setup.load(ActionContextBuilder::class.java.classLoader)
        }
    }
}

enum class ThreadFlag(val value: Int) {
    ASYNC(0),
    READ(1),
    WRITE(2),
    SWING(4),
    AWT(8),
    INVALID(-1)
}

interface Boundary {

    fun count(): Int

    fun close()

    fun remove()

    fun isClosed(): Boolean

    fun waitComplete(autoRemove: Boolean = true)

    fun waitComplete(msTimeout: Long, autoRemove: Boolean = true): Boolean
}

interface InnerBoundary : Boundary {

    fun down()

    fun up()

    fun removed(): Boolean
}

class BoundaryImpl(private val root: ActionContext.Companion.ContextStatus) : InnerBoundary {

    private var countLatch: CountLatch = AQSCountLatch()

    private var cnt = AtomicInteger()

    @Volatile
    private var closed = false

    @Volatile
    private var removed = false

    override fun down() {
        if (closed) {
            throw ProcessCanceledException("boundary closed")
        }
        countLatch.down()
        cnt.getAndIncrement()
    }

    override fun up() {
        countLatch.up()
        cnt.getAndDecrement()
    }

    override fun removed(): Boolean {
        return this.removed
    }

    override fun count(): Int {
        return cnt.get()
    }

    override fun close() {
        closed = true
    }

    override fun remove() {
        this.removed = true
        root.removeBoundary(this)
    }

    override fun isClosed(): Boolean {
        return closed
    }

    /**
     * waits on the sub thread for this Boundary
     */
    override fun waitComplete(autoRemove: Boolean) {
        try {
            if (ActionContext.getFlag() != 0) {
                LOG.warn("don't waitComplete at ui thread!")
                this.countLatch.waitFor(200)
                return
            }
            this.countLatch.waitFor()
        } finally {
            if (autoRemove) {
                this.remove()
            }
        }
    }

    override fun waitComplete(msTimeout: Long, autoRemove: Boolean): Boolean {
        try {
            if (ActionContext.getFlag() != 0) {
                LOG.warn("don't waitComplete at ui thread!")
                return false
            }
            return this.countLatch.waitFor(msTimeout)
        } finally {
            if (autoRemove) {
                this.remove()
            }
        }
    }
}

class InnerBoundaries(private var boundaries: List<InnerBoundary>) : InnerBoundary {

    override fun down() {
        var error: Throwable? = null
        var index = 0
        while (index < boundaries.size) {
            try {
                boundaries[index].down()
                index++
            } catch (e: Exception) {
                error = e
                break
            }
        }
        if (error != null) {
            //roll back
            for (i in 0 until index) {
                boundaries[i].up()
            }
            throw error
        }
    }

    override fun up() {
        boundaries.forEach { it.up() }
    }

    override fun removed(): Boolean {
        return boundaries.all { it.removed() }
    }

    override fun count(): Int {
        return boundaries.sumOf { it.count() }
    }

    override fun close() {
        boundaries.forEach { it.close() }
    }

    override fun remove() {
        boundaries.forEach { it.remove() }
    }

    override fun isClosed(): Boolean {
        return boundaries.any { it.isClosed() }
    }

    override fun waitComplete(autoRemove: Boolean) {
        boundaries.forEach {
            it.waitComplete(autoRemove)
        }
    }

    override fun waitComplete(msTimeout: Long, autoRemove: Boolean): Boolean {
        val timeOut = System.currentTimeMillis() + msTimeout
        for (boundary in boundaries) {
            if (System.currentTimeMillis() > timeOut) {
                if (autoRemove) {
                    boundary.remove()
                    continue
                }
            }
            if (!boundary.waitComplete(msTimeout, autoRemove)) {
                return false
            }
        }
        return System.currentTimeMillis() < timeOut
    }
}

//background idea log
private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ActionContext::class.java)
