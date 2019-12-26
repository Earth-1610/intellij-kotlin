package com.itangcent.intellij.extend.guice

import java.lang.reflect.Field


/**
 * Allows a method with a given annotation `A` on an injectee of type `I` to be
 * processed in some way on each injectee using a custom strategy.
 *
 * @version $Revision: 1.1 $
 */
interface FieldHandler<in I, in A : Annotation> {

    /**
     * Process the given method which is annotated with the annotation
     * on the injectee after the injectee has been injected
     */
//    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    fun afterInjection(injectee: I, annotation: A, field: Field)
}
