package com.itangcent.common.utils

object JavaClassHelper {

//    private val EMPTY_CLASS_ARRAYCLASS_ARRAY = arrayOfNulls<Class<*>>(0)

    fun <T> newInstance(type: Class<out T>): T {
        try {
            val constructor = type.getConstructor()
            return constructor.newInstance()
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Can't initialize class ${type.name}, no <init>()", e)
        }
    }

    fun <T> newInstance(type: Class<out T>, vararg params: Pair<Any, Class<*>>): T {
        val (argList, argTypes) = params.unzip()

        try {
            val constructor = type.getConstructor(*argTypes.toTypedArray())
            return constructor.newInstance(*argList.toTypedArray())
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Can't initialize class ${type.name}, no <init>(${argTypes.joinToString()})", e)
        }
    }

//
//    fun <T> newInstance(type: Class<out T>, vararg params: Any): Any {
//        var constructor: Constructor<*>? = null
//        try {
//            if (params.isEmpty()) {
//                constructor = type.getConstructor(*ClassHelper.EMPTY_CLASS_ARRAY)
//            } else {
//                val paramTypes: Array<Class<*>> =
//                        params.toList()
//                                .stream()
//                                .map { param -> param::class.java }
//                                .toTypedArray()
//                constructor = type.getConstructor(*paramTypes)
//            }
//        } catch (e: NoSuchMethodException) {
//            // ignore
//        }
//
//
//        if (constructor == null) {
//            val constructors = type.constructors
//            if (constructors == null || constructors.size == 0) {
//                throw IllegalArgumentException("Illegal params to instance Class[" + type.name + "]")
//            }
//            for (cst in constructors) {
//                val paramCls = cst.parameterTypes
//                if (isAssignable(params, paramCls)) {
//                    constructor = cst
//                    break
//                }
//            }
//        }
//        if (constructor == null) {
//            throw IllegalArgumentException("Illegal params to instance Class[" + type.name + "]")
//        }
//        try {
//            return constructor.newInstance(*params)
//        } catch (e: InstantiationException) {
//            throw IllegalArgumentException("Illegal params to instance Class[" + type.name + "]")
//        } catch (e: IllegalAccessException) {
//            throw IllegalArgumentException("Illegal params to instance Class[" + type.name + "]")
//        } catch (e: InvocationTargetException) {
//            throw IllegalArgumentException("Illegal params to instance Class[" + type.name + "]")
//        }
//
//    }

}
