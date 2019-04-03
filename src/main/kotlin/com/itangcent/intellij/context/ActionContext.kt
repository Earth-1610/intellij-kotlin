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
import com.itangcent.common.utils.IDUtils
import com.itangcent.common.utils.ThreadPoolUtils
import com.itangcent.intellij.constant.CacheKey
import com.itangcent.intellij.extend.guice.KotlinModule
import com.itangcent.intellij.extend.guice.instance
import com.itangcent.intellij.extend.guice.singleton
import org.aopalliance.intercept.MethodInterceptor
import java.awt.EventQueue
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

/**
 * Action上下文
 * 包含一个guice的injector，来管理所有生成的实例
 * 包含一个CountLatch，来维持子进程状态
 */
class ActionContext {

    private val cache = HashMap<String, Any?>()

    private val lock = ReentrantReadWriteLock()

    private val id = IDUtils.shortUUID()

    @Volatile
    private var locked = false

    private var countLatch: CountLatch = AQSCountLatch()

    private var executorService: ExecutorService = ThreadPoolUtils.createPool(5, ActionContext::class.java)

    //使用guice管理当前上下文实例生命周期与依赖
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
        lock.writeLock().withLock { cache.put(cachePrefix + name, bean) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCache(name: String): T? {
        return lock.readLock().withLock { cache[cachePrefix + name] as T? }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> cacheOrCompute(name: String, beanSupplier: () -> T?): T? {
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
    fun on(name: String, event: Runnable) {
        lock.writeLock().withLock {
            val key = eventPrefix + name
            val oldEvent: Runnable? = cache[key] as Runnable?
            if (oldEvent == null) {
                cache[key] = event
            } else {
                cache[key] = Runnable {
                    oldEvent.run()
                    event.run()
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

    //锁住缓存
    fun lock(): Boolean = lock.writeLock().withLock {
        return if (locked) {
            false
        } else {
            locked = true
            ActionContext.setContext(this)
            true
        }
    }

    fun hold() {
        countLatch.down()
    }

    fun unHold() {
        countLatch.up()
    }

    fun runAsync(runnable: Runnable) {
        countLatch.down()
        executorService.submit {
            try {
                ActionContext.setContext(this, 0)
                runnable.run()
            } finally {
                ActionContext.clearContext()
                countLatch.up()
            }
        }
    }

    fun runAsync(runnable: () -> Unit) {
        countLatch.down()
        executorService.submit {
            try {
                ActionContext.setContext(this, 0)
                runnable()
            } finally {
                ActionContext.clearContext()
                countLatch.up()
            }
        }
    }

    fun runInSwingUI(runnable: () -> Unit) {
        if (getFlag() == swingThreadFlag) {
            runnable()
        } else {
            countLatch.down()
            EventQueue.invokeLater {
                try {
                    ActionContext.setContext(this, swingThreadFlag)
                    runnable()
                } finally {
                    ActionContext.clearContext()
                    countLatch.up()
                }
            }
        }
    }

    fun <T> callInSwingUI(callable: () -> T?): T? {
        if (getFlag() == swingThreadFlag) {
            return callable()
        } else {
            countLatch.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            EventQueue.invokeLater {
                try {
                    ActionContext.setContext(this, swingThreadFlag)
                    valueHolder.compute { callable() }
                } finally {
                    ActionContext.clearContext()
                    countLatch.up()
                }
            }
            return valueHolder.getData()
        }
    }

    fun runInWriteUI(runnable: () -> Unit) {
        if (getFlag() == writeThreadFlag) {
            runnable()
        } else {
            val project = this.instance(Project::class)
            countLatch.down()
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    ActionContext.setContext(this, writeThreadFlag)
                    runnable()
                } finally {
                    ActionContext.clearContext()
                    countLatch.up()
                }
            }
        }
    }

    fun <T> callInWriteUI(callable: () -> T?): T? {
        if (getFlag() == writeThreadFlag) {
            return callable()
        } else {
            val project = this.instance(Project::class)
            countLatch.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    ActionContext.setContext(this, writeThreadFlag)
                    valueHolder.compute { callable() }
                } finally {
                    ActionContext.clearContext()
                    countLatch.up()
                }
            }
            return valueHolder.getData()
        }
    }

    fun runInReadUI(runnable: () -> Unit) {

        if (getFlag() == readThreadFlag) {
            runnable()
        } else {
            countLatch.down()
            ReadAction.run<Throwable> {
                try {
                    ActionContext.setContext(this, readThreadFlag)
                    runnable()
                } finally {
                    ActionContext.clearContext()
                    countLatch.up()
                }
            }
        }
    }

    fun <T> callInReadUI(callable: () -> T?): T? {
        if (getFlag() == readThreadFlag) {
            return callable()
        } else {
            countLatch.down()
            val valueHolder: ValueHolder<T> = ValueHolder()
            ReadAction.run<Throwable> {
                try {
                    ActionContext.setContext(this, readThreadFlag)
                    valueHolder.compute { callable() }
                } finally {
                    ActionContext.clearContext()
                    countLatch.up()
                }
            }
            return valueHolder.getData()
        }
    }

    /**
     * 等待完成
     * warning:调用waitComplete*方法将清除当前线程绑定的ActionContext
     * @see ActionContext.waitCompleteAsync
     */
    fun waitComplete() {
        ActionContext.clearContext()
        this.countLatch.waitFor()
        this.call(CacheKey.ONCOMPLETED)
        lock.writeLock().withLock {
            this.cache.clear()
            locked = false
        }
    }

    /**
     * 主线程完成,在异步线程上等待子线程完成
     * warning:调用waitComplete*方法将清除当前线程绑定的ActionContext
     * @see ActionContext.waitComplete
     */
    fun waitCompleteAsync() {
        ActionContext.clearContext()
        executorService.submit {
            this.countLatch.waitFor()
            this.call(CacheKey.ONCOMPLETED)
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

    companion object {

        private const val readThreadFlag = 0b0001
        private const val writeThreadFlag = 0b0010
        private const val swingThreadFlag = 0b0100

        private const val cachePrefix = "cache_"
        private const val eventPrefix = "event_"

        private var localContext: ThreadLocal<ThreadLocalContext> = ThreadLocal()

        /**
         * 获得当前线程上下文
         */
        public fun getContext(): ActionContext? {
            return localContext.get()?.actionContext
        }

        public fun getFlag(): Int {
            return localContext.get()?.flag ?: 0
        }

        public fun builder(): ActionContextBuilder {
            return ActionContextBuilder()
        }

        private fun setContext(actionContext: ActionContext) {
            setContext(actionContext, getFlag())
        }

        private fun setContext(actionContext: ActionContext, flag: Int) {
            val existContext = localContext.get()
            if (existContext == null) {
                localContext.set(ThreadLocalContext(actionContext, flag, 1))
            } else {
                existContext.addCount()
            }
        }

        private fun clearContext() {

            val existContext = localContext.get()
            if (existContext != null) {
                if (existContext.releaseCount() == 0) {
                    localContext.remove()
                }
            }
        }

        /**
         * 声明一个本地代理对象，它将在使用时从使用它的线程中获取上下文中的此类型的相应对象
         */
        public inline fun <reified T : Any> local() = ThreadLocalContextBeanProxies.instance(T::class)

        public fun <T : Any> instance(clazz: KClass<T>): T {
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

    class ActionContextBuilder : ModuleActions {
        override fun <T : Any> bind(type: KClass<T>, callBack: (LinkedBindingBuilder<T>) -> Unit) {
            moduleActions.add(arrayOf(BIND, type, callBack))
        }

        override fun <T : Any> bind(
            type: KClass<T>,
            annotationType: Class<out Annotation>,
            callBack: (LinkedBindingBuilder<T>) -> Unit
        ) {
            moduleActions.add(arrayOf(BIND_WITH_ANNOTATION_TYPE, type, annotationType, callBack))
        }

        override fun <T : Any> bind(
            type: KClass<T>,
            annotation: Annotation,
            callBack: (LinkedBindingBuilder<T>) -> Unit
        ) {
            moduleActions.add(arrayOf(BIND_WITH_ANNOTATION, type, annotation, callBack))
        }

        override fun <T : Any> bind(type: KClass<T>, namedText: String, callBack: (LinkedBindingBuilder<T>) -> Unit) {
            moduleActions.add(arrayOf(BIND_WITH_NAME, type, namedText, callBack))
        }

        override fun <T : Any> bindInstance(name: String, instance: T) {
            moduleActions.add(arrayOf(BIND_INSTANCE_WITH_NAME, name, instance))
        }

        override fun <T> bindInstance(instance: T) {
            moduleActions.add(arrayOf<Any>(BIND_INSTANCE, instance!!))
        }

        override fun <T : Any> bindInstance(cls: KClass<T>, instance: T) {
            moduleActions.add(arrayOf<Any>(BIND_INSTANCE_WITH_CLASS, cls, instance))
        }

        override fun bindInterceptor(
            classMatcher: Matcher<in Class<*>>,
            methodMatcher: Matcher<in Method>,
            vararg interceptors: MethodInterceptor
        ) {
            moduleActions.add(arrayOf<Any>(BIND_INTERCEPTOR, classMatcher, methodMatcher, interceptors))
        }

        override fun bindConstant(callBack: (AnnotatedConstantBindingBuilder) -> Unit) {
            moduleActions.add(arrayOf<Any>(BIND_CONSTANT, callBack))
        }

        private val appendModules: MutableList<Module> = ArrayList()
        private val moduleActions: MutableList<Array<Any>> = ArrayList()

        fun addModule(vararg modules: Module) {
            this.appendModules.addAll(modules)
        }

        fun build(): ActionContext {
            if (moduleActions.isNotEmpty()) {
                appendModules.add(ConfiguredModule(ArrayList(moduleActions)))
                moduleActions.clear()
            }
            return ActionContext(*appendModules.toTypedArray())
        }

        companion object {
            const val BIND_WITH_ANNOTATION_TYPE = "bindWithAnnotationType"
            const val BIND_WITH_ANNOTATION = "bindWithAnnotation"
            const val BIND = "bind"
            const val BIND_WITH_NAME = "bindWithName"
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
                        (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit)).invoke(
                            bind(moduleAction[1] as KClass<*>, moduleAction[2] as Class<Annotation>)
                        )
                    }
                    ActionContextBuilder.BIND_WITH_ANNOTATION -> {
                        (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit)).invoke(
                            bind(moduleAction[1] as KClass<*>, moduleAction[2] as Annotation)
                        )
                    }
                    ActionContextBuilder.BIND_WITH_NAME -> {
                        (moduleAction[3] as ((LinkedBindingBuilder<*>) -> Unit)).invoke(
                            bind(moduleAction[1] as KClass<*>, moduleAction[2] as String)
                        )
                    }
                    ActionContextBuilder.BIND_INSTANCE_WITH_NAME -> {
                        bindInstance(moduleAction[1] as String, moduleAction[2])
                    }
                    ActionContextBuilder.BIND_INSTANCE -> {
                        bindInstance(moduleAction[1])
                    }
                    ActionContextBuilder.BIND_INSTANCE_WITH_CLASS -> {
                        bindInstance(moduleAction[1] as KClass<Any>, moduleAction[2])
                    }
                    ActionContextBuilder.BIND -> {
                        (moduleAction[2] as ((LinkedBindingBuilder<*>) -> Unit)).invoke(
                            bind(moduleAction[1] as KClass<*>)
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

        fun <T : Any> bind(type: KClass<T>, callBack: (LinkedBindingBuilder<T>) -> Unit)

        fun <T : Any> bind(
            type: KClass<T>, annotationType: Class<out Annotation>
        ) {
            bind(type, annotationType) { it.singleton() }
        }

        fun <T : Any> bind(
            type: KClass<T>,
            annotationType: Class<out Annotation>,
            callBack: ((LinkedBindingBuilder<T>) -> Unit)
        )

        fun <T : Any> bind(type: KClass<T>, annotation: Annotation) {
            bind(type, annotation) { it.singleton() }
        }

        fun <T : Any> bind(type: KClass<T>, annotation: Annotation, callBack: ((LinkedBindingBuilder<T>) -> Unit))

        fun <T : Any> bind(type: KClass<T>, namedText: String) {
            bind(type, namedText) { it.singleton() }
        }

        fun <T : Any> bind(type: KClass<T>, namedText: String, callBack: ((LinkedBindingBuilder<T>) -> Unit))

        fun <T : Any> bindInstance(name: String, instance: T)

        fun <T> bindInstance(instance: T)

        fun <T : Any> bindInstance(cls: KClass<T>, instance: T)

        fun bindInterceptor(
            classMatcher: Matcher<in Class<*>>,
            methodMatcher: Matcher<in Method>,
            vararg interceptors: org.aopalliance.intercept.MethodInterceptor
        )

        fun bindConstant(callBack: (AnnotatedConstantBindingBuilder) -> Unit)
    }

}
