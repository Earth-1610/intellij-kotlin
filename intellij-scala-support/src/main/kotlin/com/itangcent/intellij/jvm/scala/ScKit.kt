package com.itangcent.intellij.jvm.scala

import scala.Option
import java.util.*

@Suppress("UNCHECKED_CAST")
fun <T> Any?.getOrNull(): T? {
    (this as? Option<*>)?.takeIf { it.isDefined }?.let { return it.get() as? T? }
    (this as? Optional<*>)?.takeIf { it.isPresent }?.let { return it.get() as? T? }

    return this as? T?
}

fun Any.castToList(): List<Any> {
    (this as? scala.collection.Iterable<*>)?.toKtList()?.filterNotNull()?.let { return it }
    (this as? scala.collection.Iterator<*>)?.toKtList()?.filterNotNull()?.let { return it }
    (this as? Iterator<*>)?.toList()?.filterNotNull()?.let { return it }
    (this as? Iterable<*>)?.toList()?.filterNotNull()?.let { return it }
    return emptyList()
}


@Suppress("UNCHECKED_CAST")
fun <T : Any> Any.castToTypedList(): List<T> {
    (this as? scala.collection.Iterable<T?>)?.toKtList()?.filterNotNull()?.let { return it }
    (this as? scala.collection.Iterator<T>)?.toKtList()?.let { return it }
    (this as? Iterator<T?>)?.toList()?.filterNotNull()?.let { return it }
    (this as? Iterable<T?>)?.toList()?.filterNotNull()?.let { return it }
    return emptyList()
}

private fun <T> Iterator<T>.toList(): List<T> {
    val result = LinkedList<T>()
    this.forEach { result.add(it) }
    return result
}

fun <T> scala.collection.Iterable<T>.toKtList(): List<T> {
    val result = LinkedList<T>()
    for (r in this) {
        result.add(r)
    }
    return result
}

fun <T : Any> scala.collection.Iterator<T>.toKtList(): List<T> {
    val result = LinkedList<T>()
    this.forall { result.add(it) }
    return result
}