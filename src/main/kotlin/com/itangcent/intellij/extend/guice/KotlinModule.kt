package com.itangcent.intellij.extend.guice

import com.google.inject.AbstractModule
import com.google.inject.Key
import com.google.inject.ProvisionException
import com.google.inject.TypeLiteral
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.matcher.Matchers
import com.google.inject.name.Names
import com.google.inject.spi.InjectionListener
import com.google.inject.spi.TypeEncounter
import com.google.inject.spi.TypeListener
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

open class KotlinModule : AbstractModule() {

    /** @see Binder.bind
     */
    fun <T : Any> bind(kClass: KClass<T>): AnnotatedBindingBuilder<T> {
        return this.bind(kClass.java)
    }

    /**
     * Binds a post injection hook method annotated with the given annotation to the given method
     * handler.
     */
    protected fun <A : Annotation> bindMethodHandler(
        annotationType: Class<A>,
        methodHandler: MethodHandler<Any?, Annotation>
    ) {
        bindMethodHandler(annotationType, EncounterProvider.encounterProvider(methodHandler))
    }

    private fun <A : Annotation> bindMethodHandler(
        annotationType: Class<A>,
        encounterProvider: EncounterProvider<MethodHandler<Any?, Annotation>>
    ) {
        bindListener(Matchers.any(), object : TypeListener {
            override fun <I> hear(injectableType: TypeLiteral<I>, encounter: TypeEncounter<I>) {
                // #todo:provider暂时无法完成PostConstruct
                val type = injectableType.rawType
                val methods = type.declaredMethods
                for (method in methods) {
                    val annotation = method.getAnnotation(annotationType)
                    if (annotation != null) {
                        val provider = encounterProvider.get(encounter)

                        encounter.register(InjectionListener { injectee ->
                            val methodHandler = provider.get()
                            try {
                                methodHandler.afterInjection(injectee, annotation, method)
                            } catch (ie: InvocationTargetException) {
                                val e = ie.targetException
                                throw ProvisionException(e.message, e)
                            } catch (e: IllegalAccessException) {
                                throw ProvisionException(e.message, e)
                            }
                        })
                    }
                }
            }
        })
    }

    /**
     * A helper method to bind the given type with the binding annotation.
     *
     * This allows you to replace this code ` bind(Key.get(MyType.class, SomeAnnotation.class))
    `*
     *
     * with this ` bind(KMyType.class, SomeAnnotation.class) `
     */
    protected fun <T : Any> bind(
        type: KClass<T>,
        annotationType: Class<out Annotation>
    ): LinkedBindingBuilder<T> {
        return bind(Key.get(type.java, annotationType))
    }

    /**
     * A helper method to bind the given type with the binding annotation.
     *
     * This allows you to replace this code ` bind(Key.get(MyType.class, SomeAnnotation.class))
    `*
     *
     * with this ` bind(KMyType.class, SomeAnnotation.class) `
     */
    protected fun <T : Any> bind(
        type: Class<T>,
        annotationType: Class<out Annotation>
    ): LinkedBindingBuilder<T> {
        return bind(Key.get(type, annotationType))
    }

    /**
     * A helper method to bind the given type with the binding annotation.
     *
     * This allows you to replace this code ` bind(Key.get(MyType.class, someAnnotation))
    ` *
     *
     * with this ` bind(KMyType.class, someAnnotation) `
     */
    protected fun <T : Any> bind(type: Class<T>, annotation: Annotation): LinkedBindingBuilder<T> {
        return bind(Key.get(type, annotation))
    }

    /**
     * A helper method to bind the given type with the binding annotation.
     *
     * This allows you to replace this code ` bind(Key.get(MyType.class, someAnnotation))
    ` *
     *
     * with this ` bind(KMyType.class, someAnnotation) `
     */
    protected fun <T : Any> bind(type: KClass<T>, annotation: Annotation): LinkedBindingBuilder<T> {
        return bind(Key.get(type.java, annotation))
    }

    /**
     * A helper method to bind the given type with the [com.google.inject.name.Named] annotation
     * of the given text value.
     *
     * This allows you to replace this code ` bind(Key.get(MyType.class, Names.named("myName")))
    ` *
     *
     * with this ` bind(KMyType.class, "myName") `
     */
    protected fun <T : Any> bind(type: KClass<T>, namedText: String): LinkedBindingBuilder<T> {
        return bind(type, Names.named(namedText))
    }

    /**
     * A helper method to bind the given type with the [com.google.inject.name.Named] annotation
     * of the given text value.
     *
     * This allows you to replace this code ` bind(Key.get(MyType.class, Names.named("myName")))
    ` *
     *
     * with this ` bind(KMyType.class, "myName") `
     */
    protected fun <T : Any> bind(type: Class<T>, namedText: String): LinkedBindingBuilder<T> {
        return bind(type, Names.named(namedText))
    }

    /**
     * A helper method which binds a named instance to a key defined by the given name and the
     * instances type. So this method is short hand for
     *
     * ` bind(instance.getClass(), name).toInstance(instance); `
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : Any> bindInstance(name: String, instance: T) {
        // TODO not sure the generics ninja to avoid this cast
        val aClass: KClass<T> = instance::class as KClass<T>
        bind(aClass, name).toInstance(instance)
    }

    /**
     * A helper method which binds a named instance to a key defined by the given name and the
     * instances type. So this method is short hand for
     *
     * ` bind(instance.getClass(), name).toInstance(instance); `
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : Any> bindInstance(instance: T) {
        // TODO not sure the generics ninja to avoid this cast
        val aClass: KClass<T> = instance::class as KClass<T>
        bind(aClass).toInstance(instance)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T : Any> bindInstance(cls: KClass<T>, instance: T) {
        bind(cls).toInstance(instance)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T : Any> bindInstance(cls: Class<T>, instance: T) {
        bind(cls).toInstance(instance)
    }

    override fun configure() {

        bindMethodHandler(PostConstruct::class.java, object : MethodHandler<Any?, Annotation> {
            @Throws(InvocationTargetException::class, IllegalAccessException::class)
            override fun afterInjection(injectee: Any?, annotation: Annotation, method: Method) {
                method.invoke(injectee)
            }
        })

    }
}