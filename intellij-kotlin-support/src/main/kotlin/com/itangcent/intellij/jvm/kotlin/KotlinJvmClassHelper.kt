package com.itangcent.intellij.jvm.kotlin

import com.google.common.collect.Sets
import com.itangcent.intellij.jvm.JvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper
import com.itangcent.intellij.jvm.standard.StandardJvmClassHelper.Companion.normalTypes
import java.util.*
import kotlin.reflect.KClass

class KotlinJvmClassHelper(private val jvmClassHelper: JvmClassHelper) : JvmClassHelper by jvmClassHelper {
    companion object {

        init {

            normalTypes["Int"] = 0
            normalTypes["kotlin.Int"] = 0

            normalTypes["Byte"] = 0
            normalTypes["kotlin.Byte"] = 0

            normalTypes["Short"] = 0
            normalTypes["kotlin.Short"] = 0

            normalTypes["Long"] = 0
            normalTypes["kotlin.Long"] = 0

            normalTypes["Float"] = 0.0
            normalTypes["kotlin.Float"] = 0.0

            normalTypes["Double"] = 0.0
            normalTypes["kotlin.Double"] = 0.0

            normalTypes["kotlin.String"] = ""

            val collectionClasses = Sets.newHashSet(*StandardJvmClassHelper.collectionClasses!!)
            addClass(kotlin.collections.Collection::class, collectionClasses)
            addClass(MutableCollection::class, collectionClasses)
            addClass(kotlin.collections.Set::class, collectionClasses)
            addClass(MutableSet::class, collectionClasses)
            addClass(kotlin.collections.List::class, collectionClasses)
            addClass(MutableList::class, collectionClasses)
            StandardJvmClassHelper.collectionClasses = collectionClasses.toTypedArray()

            val mapClasses = Sets.newHashSet(*StandardJvmClassHelper.mapClasses!!)
            addClass(kotlin.collections.Map::class, mapClasses)
            addClass(MutableMap::class, mapClasses)
            StandardJvmClassHelper.mapClasses = mapClasses.toTypedArray()

        }

        private fun addClass(cls: KClass<*>, classSet: HashSet<String>) {
            classSet.add(cls.qualifiedName!!)
            classSet.add(cls.simpleName!!)
        }
    }
}